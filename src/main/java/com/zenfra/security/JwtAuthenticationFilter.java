package com.zenfra.security;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.zenfra.configuration.RedisUtil;
import com.zenfra.utils.TrippleDes;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;


public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    RedisUtil redisUtil;

    @Autowired
    TrippleDes tripple;
    
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
    	//System.out.println("!!!!! Constants.HEADER_STRING: " + Constants.HEADER_STRING);
    	//System.out.println("!!!!! headers: " + req.getHeaderNames());
        String header = req.getHeader(Constants.HEADER_STRING);
        //System.out.println("!!!!! Header: " + header);
        
        String path = req.getRequestURI();
        String contentType = req.getContentType();
        System.out.println("Request URL path="+path+": Request content type ="+contentType);
        
         
        /*if(path!=null && path.contains("/auth")) {
        	return;
        }*/
        
        System.out.println("----------------Enter do fillter-------------");
        String username = null;
        String authToken = null;
        System.out.println("!!!!! header: " + header);
        if (header != null && header.startsWith(Constants.TOKEN_PREFIX)) {
            authToken = header.replace(Constants.TOKEN_PREFIX,"");
            System.out.println("!!!!! authToken: " + authToken);
            try {
                username = jwtTokenUtil.getUsernameFromToken(authToken);
                System.out.println("!!!!! username: " + username);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (ExpiredJwtException e) {
            	e.printStackTrace();
            } catch(SignatureException e){
            	e.printStackTrace();
            }
        } else {
            logger.warn("couldn't find bearer string, will ignore the header");
        }
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

        	
        	
        	String userObject=redisUtil.getValue(authToken)!=null ? redisUtil.getValue(authToken).toString() : null;
            if(userObject!=null &&  !userObject.isEmpty()) {   
            	String userId = jwtTokenUtil.getUserId(authToken);
            	req.setAttribute("authUserId", userId);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userObject, null, Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authentication);
        	
            }else {
            	 UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
		            if(jwtTokenUtil.validateToken(authToken, userDetails)) {
		            	String userId = jwtTokenUtil.getUserId(authToken);
		            	req.setAttribute("authUserId", userId);
		                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
		                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
		                SecurityContextHolder.getContext().setAuthentication(authentication);
		            }
            }
        }

        chain.doFilter(req, res);
    }
}
