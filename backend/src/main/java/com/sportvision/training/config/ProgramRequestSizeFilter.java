package com.sportvision.training.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvision.training.dto.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
/** Limite technique de 1 Mio avant désérialisation, y compris en transfert chunked. */
@Component
public class ProgramRequestSizeFilter extends OncePerRequestFilter {
    private static final int MAX_BYTES=1024*1024;
    private final ObjectMapper mapper;
    public ProgramRequestSizeFilter(ObjectMapper mapper){this.mapper=mapper;}
    @Override protected boolean shouldNotFilter(HttpServletRequest request){
        return !(request.getServletPath().startsWith("/programs")||request.getServletPath().startsWith("/ai/")) || !java.util.Set.of("POST","PUT","PATCH").contains(request.getMethod());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        byte[] body=request.getInputStream().readNBytes(MAX_BYTES+1);
        if(body.length>MAX_BYTES){
            response.setStatus(400);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");
            mapper.writeValue(response.getWriter(),new ApiResponse<>(false,"Requête limitée à 1 Mio.",Map.of("errors",Map.of())));return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request){
            @Override public ServletInputStream getInputStream(){
                ByteArrayInputStream stream=new ByteArrayInputStream(body);
                return new ServletInputStream(){
                    public int read(){return stream.read();}
                    public boolean isFinished(){return stream.available()==0;}
                    public boolean isReady(){return true;}
                    public void setReadListener(ReadListener listener){throw new UnsupportedOperationException("Synchronous request only");}
                };
            }
            @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}
        },response);
    }
}

