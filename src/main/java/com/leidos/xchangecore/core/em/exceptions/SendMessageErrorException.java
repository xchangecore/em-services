package com.leidos.xchangecore.core.em.exceptions;

import java.util.HashMap;
import java.util.Map;

import com.leidos.xchangecore.core.infrastructure.exceptions.UICDSException;

@SuppressWarnings("serial")
public class SendMessageErrorException
    extends UICDSException {

    public enum SEND_MESSAGE_ERROR_TYPE {
        CORE_UNKNOWN, CORE_UNAVAILABLE, NO_SHARE_AGREEMENT, NO_SHARE_RULE_IN_AGREEMENT
    }

    // key: coreName
    // value: errorType
    Map<String, SEND_MESSAGE_ERROR_TYPE> errors = new HashMap<String, SEND_MESSAGE_ERROR_TYPE>();

    public Map<String, SEND_MESSAGE_ERROR_TYPE> getErrors() {

        return errors;
    }

    public void setErrors(Map<String, SEND_MESSAGE_ERROR_TYPE> errors) {

        this.errors = errors;
    }

}
