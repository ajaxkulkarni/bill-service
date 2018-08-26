package com.rns.web.billapp.service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;

import com.rns.web.billapp.service.util.BillPropertyUtil;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

@Provider
public class AuthFilter implements ContainerRequestFilter {

	private static final String AUTHENTICATION_HEADER = "Token";

	/*public void filter(ContainerRequestContext requestContext) throws IOException {
		if(StringUtils.contains("admin", requestContext.getMethod())) {
			String authCredentials = requestContext.getHeaderString(AUTHENTICATION_HEADER);
			System.out.println("Header .." + authCredentials);
			// better injected
			AuthenticationService authenticationService = new AuthenticationService();

			boolean authenticationStatus = authenticationService.authenticate(authCredentials);

			if (!validToken(authCredentials)) {
				throw new WebApplicationException(Status.UNAUTHORIZED);
			}
		}
	}*/

	private boolean validToken(String authCredentials) {
		return (StringUtils.equals(BillPropertyUtil.getProperty(BillPropertyUtil.ADMIN_TOKEN), authCredentials));
	}

	public ContainerRequest filter(ContainerRequest request) {
		System.out.println("Here!");
		if(StringUtils.equalsIgnoreCase("GET", request.getMethod())) {
			return request;
		}
		if(StringUtils.contains(request.getRequestUri().getPath(), "/admin/" ) && !StringUtils.contains(request.getRequestUri().getPath(), "/login" )) {
			String authCredentials = request.getHeaderValue(AUTHENTICATION_HEADER);
			System.out.println("Header .." + authCredentials);
			// better injected
			/*AuthenticationService authenticationService = new AuthenticationService();

			boolean authenticationStatus = authenticationService.authenticate(authCredentials);*/

			if (!validToken(authCredentials)) {
				throw new WebApplicationException(Status.UNAUTHORIZED);
			}
			
		}
		return request;
	}

}
