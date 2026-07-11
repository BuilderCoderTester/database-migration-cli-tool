package com.project.demo.BeforeExecutionValidation.parser;

import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import net.sf.jsqlparser.statement.drop.Drop;
import org.springframework.stereotype.Component;

@Component
public class DropTableParser {

    public void parse(
            Drop drop,
            SchemaModel schema) {

        TableModel table = new TableModel();

        table.setTableName(
                drop.getName().getName()
        );

        table.setDropped(true);

        schema.getTables().put(
                table.getTableName(),
                table
        );
    }
}