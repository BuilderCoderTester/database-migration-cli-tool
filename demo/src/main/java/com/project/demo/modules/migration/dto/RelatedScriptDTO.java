package com.project.demo.modules.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
@Getter
@Setter
@AllArgsConstructor
public class RelatedScriptDTO {
    private String version;

    private String description;

    private String type;

    private String status;

    private boolean success;
}
