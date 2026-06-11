package com.project.demo.service;

import com.project.demo.utility.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

@Service
public class SchemaMetadataService {

    @Autowired
    private Helper helper;

    public List<String> extractPrimaryKey(
            String script,
            Connection connection) {
        return helper.extractPrimaryKeys(script);
    }
}
