package com.project.demo.BeforeExecutionValidation.parser;

import com.project.demo.BeforeExecutionValidation.model.ColumnModel;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import org.springframework.stereotype.Component;

import net.sf.jsqlparser.statement.Statement;
import java.util.List;

@Component
public class ASTSchemaExtractor {
    public SchemaModel extract(String sql) throws Exception {

        Statement statement =  CCJSqlParserUtil.parse(sql);

        SchemaModel schema = new SchemaModel();

        if (statement instanceof CreateTable createTable) {
            System.out.println("yes");
            parseCreateTable(createTable, schema);

        }
        System.out.println("no");
        return schema;
    }

    private void parseCreateTable(
            CreateTable createTable,
            SchemaModel schema) {

        TableModel table = new TableModel();

        table.setTableName(
                createTable.getTable().getName()
        );

        schema.getTables().put(
                table.getTableName(),
                table
        );

        parseColumns(createTable, table);

        parseIndexes(createTable, table);

    }

    private void parseColumns(
            CreateTable createTable,
            TableModel table) {

        if (createTable.getColumnDefinitions() == null) {
            return;
        }

        for (ColumnDefinition definition :
                createTable.getColumnDefinitions()) {

            ColumnModel column = new ColumnModel();

            column.setColumnName(
                    definition.getColumnName()
            );

            column.setDataType(
                    definition.getColDataType().toString()
            );

            List<String> specs =
                    definition.getColumnSpecs();

            if (specs != null) {

                parseColumnSpecs(
                        specs,
                        column
                );

            }

            table.getColumns().put(
                    column.getColumnName(),
                    column
            );

        }

    }
    private void parseColumnSpecs(
            List<String> specs,
            ColumnModel column) {

        List<String> tokens =
                specs.stream()
                        .map(String::toUpperCase)
                        .toList();

        if(tokens.contains("PRIMARY") && tokens.contains("KEY")){
            column.setPrimaryKey(true);
        }

        if (tokens.contains("NOT")) {
            column.setNullable(false);
        }

        if (tokens.contains("UNIQUE")) {
            column.setUnique(true);
        }

        parseReference(
                specs,
                column
        );

    }
    private void parseReference(
            List<String> specs,
            ColumnModel column) {

        for (int i = 0; i < specs.size(); i++) {

            if (!specs.get(i)
                    .equalsIgnoreCase("REFERENCES")) {

                continue;
            }

            column.setForeignKey(true);

            String reference =
                    specs.get(i + 1);

            int start =
                    reference.indexOf("(");

            int end =
                    reference.indexOf(")");

            if (start > 0) {

                column.setReferencedTable(
                        reference.substring(
                                0,
                                start
                        )
                );

                column.setReferencedColumn(
                        reference.substring(
                                start + 1,
                                end
                        )
                );

            }

        }

    }
    private void parseIndexes(
            CreateTable createTable,
            TableModel table) {

        if (createTable.getIndexes() == null) {
            return;
        }

        for (Index index :
                createTable.getIndexes()) {

            switch (index.getType().toUpperCase()) {

                case "PRIMARY KEY" -> parsePrimaryKey(index, table);

                case "FOREIGN KEY" -> parseForeignKey(index, table);

            }

        }

    }
    private void parsePrimaryKey(
            Index index,
            TableModel table) {

        if (index.getColumnsNames() == null) {
            return;
        }

        for (String columnName : index.getColumnsNames()) {

            ColumnModel column =
                    table.getColumns().get(columnName);

            if (column != null) {
                column.setPrimaryKey(true);
                column.setNullable(false);
            }
        }
    }
    private void parseForeignKey(
            Index index,
            TableModel table) {

        if (index.getColumnsNames() == null ||
                index.getColumnsNames().isEmpty()) {
            return;
        }

        String fkColumn =
                index.getColumnsNames().get(0);

        ColumnModel column =
                table.getColumns().get(fkColumn);

        if (column == null) {
            return;
        }

        column.setForeignKey(true);

        String sql = index.toString();

        if (!sql.toUpperCase().contains("REFERENCES")) {
            return;
        }

        String reference =
                sql.substring(
                                sql.toUpperCase().indexOf("REFERENCES") +
                                        "REFERENCES".length())
                        .trim();

        int start =
                reference.indexOf("(");

        int end =
                reference.indexOf(")");

        if (start > 0 && end > start) {

            column.setReferencedTable(
                    reference.substring(0, start).trim()
            );

            column.setReferencedColumn(
                    reference.substring(start + 1, end).trim()
            );
        }
    }
    private ColumnModel getColumn(
            TableModel table,
            String columnName) {

        return table.getColumns().get(columnName);
    }
}
