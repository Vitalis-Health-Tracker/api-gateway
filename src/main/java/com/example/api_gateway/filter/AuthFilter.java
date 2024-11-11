package com.example.api_gateway.filter;

import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config>{

    @Autowired
    private RouteValidator routeValidator;

    public AuthFilter(){
        super(Config.class);
    }

    public static class Config{

    }

    RestClient restClient = RestClient.create();

    public GatewayFilter apply(Config config)
    {
        return((exchange, chain) -> {
            List<String> rolesList = getRoleList();
            if(routeValidator.isSecured.test(exchange.getRequest()))
            {
                if(!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new RuntimeException("Missing authorization header");
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);

                if(authHeader != null && authHeader.startsWith("Bearer"))
                {
                    authHeader = authHeader.substring(7);
                }

                String role = getRole(authHeader);

                try{
                    restClient.get().uri("http://localhost:9090/auth/validate/" + authHeader)
                            .retrieve().body(Boolean.class);
                }catch(Exception e)
                {
                    throw new RuntimeException("Unauthorized access");
                }
                if(!rolesList.contains(role))
                {
                    throw new RuntimeException("Invalid Role");
                }
            }
            return chain.filter(exchange);
        });
    }

    public String getRole(String token)
    {
        return restClient.get().uri("http://localhost:9090/extract/" + token)
                .retrieve().body(String.class);
    }

    public List<String> getRoleList()
    {
        return restClient.get().uri("http://localhost:9089/roles")
                .retrieve().body(List.class);
    }
}
