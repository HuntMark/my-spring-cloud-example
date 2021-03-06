package com.example.controller;

import com.example.entity.Celebrity;
import com.example.repository.CelebrityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/celebrities")
public class CelebrityController {

    private final ApplicationContext applicationContext;
    private final CelebrityRepository repository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<Celebrity> celebrities() {
        return this.repository.findAll();
    }


    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Celebrity celebrity(@PathVariable("id") Long id) {
        return this.repository.findOne(id);
    }

    @GetMapping(value = "/random", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Celebrity randomCelebrity() {
        if (this.repository.count() == 0) {
            throw new IllegalArgumentException("No celebrities found");
        }
        return this.repository.findOne(ThreadLocalRandom.current().nextLong(1, repository.count()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/random-with-test-property", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Celebrity randomCelebrityWithTestProperty() {
        TokenStore tokenStore;
        try {
            tokenStore = this.applicationContext.getBean(TokenStore.class);
        } catch (NoSuchBeanDefinitionException ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw ex;
        }

        Celebrity celebrity = randomCelebrity();

        OAuth2Authentication auth = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthenticationDetails authDetails = (OAuth2AuthenticationDetails) auth.getDetails();
        OAuth2AccessToken accessToken = tokenStore.readAccessToken(authDetails.getTokenValue());
        Map<String, Object> jwtAdditionalInformation = accessToken.getAdditionalInformation();

        String testPropertyValue = (String) jwtAdditionalInformation.get("testProperty");

        celebrity.setName(celebrity.getName() + " ; Test Property Value: " + testPropertyValue);
        return celebrity;
    }
}
