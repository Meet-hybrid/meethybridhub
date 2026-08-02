package com.meethybridhub.identity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.*;

/**
 * Custom validation annotation for password strength.
 *
 * Enforces minimum security requirements:
 *   - At least 8 characters
 *   - At least one uppercase letter
 *   - At least one lowercase letter
 *   - At least one digit
 *   - At least one special character
 */
@Size(min = 8, message = "Password must be at least 8 characters long")
@Pattern.List({
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter"),
    @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter"),
    @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one digit"),
    @Pattern(regexp = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*", 
             message = "Password must contain at least one special character")
})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidPassword {
    
    String message() default "Password does not meet security requirements";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}