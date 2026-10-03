import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="contact-page">
      <section class="contact-header">
        <div class="container">
          <span class="badge-sv badge-white">SUPPORT ATHLÈTES</span>
          <h1 class="contact-title">Contact & Centre d'Assistance</h1>
          <p class="contact-subtitle">Une question sur vos verres optiques, un équipement ou une commande ? Nos conseillers sportifs vous répondent en moins de 2h.</p>
        </div>
      </section>

      <div class="container contact-body">
        <div class="contact-grid">
          <!-- Formulaire de contact -->
          <div class="sv-card form-card">
            <h3 class="form-title">Envoyez-nous un message</h3>

            <div class="alert-success" *ngIf="messageSent">
              ✓ Merci pour votre message ! Un conseiller Sport Vision vous recontactera très rapidement.
            </div>

            <form (ngSubmit)="sendMessage()" #contactForm="ngForm" *ngIf="!messageSent">
              <div class="form-group-sv">
                <label class="form-label-sv">Votre Nom complet *</label>
                <input type="text" [(ngModel)]="name" name="name" required placeholder="Julien Bernard" class="form-control-sv">
              </div>

              <div class="form-group-sv">
                <label class="form-label-sv">Adresse email *</label>
                <input type="email" [(ngModel)]="email" name="email" required placeholder="julien@sportvision.com" class="form-control-sv">
              </div>

              <div class="form-group-sv">
                <label class="form-label-sv">Sujet de la demande</label>
                <select [(ngModel)]="subject" name="subject" class="form-control-sv">
                  <option value="optics">Conseil verres optiques & photochromiques</option>
                  <option value="order">Suivi de ma commande</option>
                  <option value="club">Partenariat Club & Équipe</option>
                  <option value="warranty">Garantie & Service après-vente</option>
                </select>
              </div>

              <div class="form-group-sv">
                <label class="form-label-sv">Votre message détaillé *</label>
                <textarea [(ngModel)]="message" name="message" required rows="5" placeholder="Décrivez votre besoin sportif..." class="form-control-sv"></textarea>
              </div>

              <button type="submit" [disabled]="!contactForm.form.valid" class="btn-sv btn-sv-primary btn-sv-lg btn-block">
                Envoyer ma demande
              </button>
            </form>
          </div>

          <!-- Coordonnées & FAQ -->
          <div class="info-sidebar">
            <div class="sv-card contact-info-card">
              <h3 class="info-title">Nos Coordonnées</h3>
              <div class="info-item">
                <span class="info-icon">📍</span>
                <div>
                  <strong>Sport Vision Flagship Store</strong>
                  <p>14 Avenue des Athlètes, 75012 Paris, France</p>
                </div>
              </div>
              <div class="info-item">
                <span class="info-icon">📞</span>
                <div>
                  <strong>Ligne Directe Athlètes</strong>
                  <p>+33 (0)1 42 00 20 26 (Lun - Sam : 8h30 - 19h30)</p>
                </div>
              </div>
              <div class="info-item">
                <span class="info-icon">✉️</span>
                <div>
                  <strong>Email Support & Devis</strong>
                  <p>contact&#64;sportvision.com</p>
                </div>
              </div>
            </div>

            <!-- FAQ Rapide -->
            <div class="sv-card faq-card">
              <h3 class="info-title">Questions Fréquentes (FAQ)</h3>
              <div class="faq-item">
                <strong>Peut-on adapter des verres correcteurs de vue ?</strong>
                <p>Oui, 90% de nos montures intègrent un insert optique clipsable ou des verres solaires taillés directement à votre vue.</p>
              </div>
              <div class="faq-item">
                <strong>Quel est le délai de livraison ?</strong>
                <p>Les commandes passées avant 14h sont expédiées le jour même via Colissimo 24h ou Chronopost.</p>
              </div>
              <div class="faq-item">
                <strong>Quelle est la politique de retour ?</strong>
                <p>Vous disposez de 30 jours pour essayer votre équipement et le retourner gratuitement s'il ne vous convient pas.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1280px;
      margin: 0 auto;
      padding: 0 20px;
    }
    .contact-header {
      background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 100%);
      color: white;
      padding: 45px 0;
      margin-bottom: 35px;
      border-bottom: 3px solid #38bdf8;
    }
    .contact-title {
      color: white;
      font-size: 2.3rem;
      margin: 10px 0 8px;
    }
    .contact-subtitle {
      color: #bfdbfe;
      font-size: 1rem;
      max-width: 650px;
    }
    .contact-grid {
      display: grid;
      grid-template-columns: 1.2fr 1fr;
      gap: 30px;
      margin-bottom: 60px;
    }
    .form-title, .info-title {
      font-size: 1.25rem;
      margin-bottom: 18px;
      padding-bottom: 10px;
      border-bottom: 1px solid #f1f5f9;
      color: #0f172a;
    }
    .btn-block {
      width: 100%;
    }
    .alert-success {
      background: #dcfce7;
      color: #15803d;
      padding: 16px;
      border-radius: 10px;
      font-weight: 600;
      margin-bottom: 20px;
    }
    .info-sidebar {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
    .info-item {
      display: flex;
      gap: 14px;
      margin-bottom: 16px;
    }
    .info-icon {
      font-size: 1.5rem;
    }
    .info-item p {
      font-size: 0.88rem;
      color: #64748b;
    }
    .faq-item {
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid #f1f5f9;
    }
    .faq-item strong {
      display: block;
      font-size: 0.92rem;
      color: #1e3a8a;
      margin-bottom: 4px;
    }
    .faq-item p {
      font-size: 0.85rem;
      color: #64748b;
      line-height: 1.5;
    }

    @media (max-width: 900px) {
      .contact-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ContactComponent {
  name: string = '';
  email: string = '';
  subject: string = 'optics';
  message: string = '';
  messageSent: boolean = false;

  sendMessage(): void {
    this.messageSent = true;
  }
}
