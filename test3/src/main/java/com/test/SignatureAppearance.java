package com.test;

import lombok.Data;

/**
 * @author Andy
 */
@Data
public class SignatureAppearance {

    private String reason;
    private String contact;
    private String location;

    private boolean visibleSignature = true;

}
