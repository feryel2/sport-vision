package com.sportvision.training.service;

import com.sportvision.training.dto.ProgramOutput;
import com.sportvision.training.entity.TrainingAiConfirmation;
import com.sportvision.training.repository.TrainingAiConfirmationRepository;
import com.sportvision.training.repository.TrainingProgramRepository;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TrainingAiConfirmationService {
    private final TrainingAiConfirmationRepository confirmations;
    private final TrainingProgramRepository programs;
    private final TrainingProgramService programService;
    private final TransactionTemplate transactions;

    public TrainingAiConfirmationService(TrainingAiConfirmationRepository confirmations,
        TrainingProgramRepository programs,TrainingProgramService programService,PlatformTransactionManager manager){
        this.confirmations=confirmations;this.programs=programs;this.programService=programService;
        transactions=new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public ProgramOutput confirm(String owner,UUID key,String hash,Supplier<ProgramOutput> create){
        try {
            return transactions.execute(status->{
                var existing=confirmations.findByOwnerReferenceAndConfirmationKey(owner,key);
                if(existing.isPresent())return replay(existing.get(),hash);
                var program=create.get();
                // Programme, révision et confirmation sont validés ou annulés ensemble.
                confirmations.saveAndFlush(new TrainingAiConfirmation(owner,key,hash,programs.getReferenceById(program.id())));
                return program;
            });
        } catch(DataIntegrityViolationException collision){
            // La contrainte unique arbitre aussi entre processus. Le perdant doit avoir
            // entièrement annulé sa création avant de relire le résultat du gagnant.
            return transactions.execute(status->{
                var winner=confirmations.findByOwnerReferenceAndConfirmationKey(owner,key);
                if(winner.isEmpty())throw collision;
                return replay(winner.get(),hash);
            });
        }
    }

    private ProgramOutput replay(TrainingAiConfirmation confirmation,String hash){
        if(!confirmation.getRequestHash().equals(hash))
            throw new TrainingAiException(409,"IDEMPOTENCY_CONFLICT","Cette clé de confirmation a déjà été utilisée avec une autre proposition.");
        // Retourne l'état courant sans recréer ni réviser le programme ; contrôle du propriétaire conservé.
        return programService.get(confirmation.getProgramId());
    }
}
