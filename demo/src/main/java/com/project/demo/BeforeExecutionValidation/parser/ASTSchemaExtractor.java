package com.project.demo.BeforeExecutionValidation.parser;

import com.project.demo.BeforeExecutionValidation.model.ColumnModel;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.Statement;
import java.util.List;
@Component
public class ASTSchemaExtractor {

    @Autowired
    private CreateTableParser createTableParser;

    @Autowired
    private AlterTableParser alterTableParser;

    @Autowired
    private DropTableParser dropTableParser;

    public SchemaModel extract(String sql) throws Exception {

        Statement statement = CCJSqlParserUtil.parse(sql);

        SchemaModel schema = new SchemaModel();

        if (statement instanceof CreateTable createTable) {

            createTableParser.parse(
                    createTable,
                    schema
            );

        } else if (statement instanceof Alter alter) {

            alterTableParser.parse(
                    alter,
                    schema
            );

        } else if (statement instanceof Drop drop) {

            dropTableParser.parse(
                    drop,
                    schema
            );
        }

        return schema;
    }
}