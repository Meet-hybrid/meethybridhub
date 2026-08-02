package com.meethybridhub.identity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.*;

/**
 * Custom validation annotation for email addresses.
 *
 * Combines standard @Email validation with additional pattern matching
 * to ensure email format consistency.
 */
@Email(message = "Please provide a valid email address")
@Pattern(regexp = ".+@.+\\..+", message = "Please provide a valid email address")
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidEmail {
    
    String message() default "Please provide a valid email address";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}