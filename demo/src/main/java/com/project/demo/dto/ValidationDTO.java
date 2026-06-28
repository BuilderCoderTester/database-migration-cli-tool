package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
@Getter
@Setter
@AllArgsConstructor
public class ValidationDTO {
    private boolean valid;

    private boolean checksumMatched;

    private boolean scriptModified;

    private boolean missingRollback;

    private boolean databaseConsistent;
}
