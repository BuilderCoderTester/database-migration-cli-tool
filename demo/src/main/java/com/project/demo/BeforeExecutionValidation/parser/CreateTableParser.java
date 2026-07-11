package com.project.demo.BeforeExecutionValidation.parser;

import com.project.demo.BeforeExecutionValidation.model.ColumnModel;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreateTableParser {

    public void parse(
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
                parseColumnSpecs(specs, column);
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

        if (tokens.contains("PRIMARY")
                && tokens.contains("KEY")) {

            column.setPrimaryKey(true);
        }

        if (tokens.contains("NOT")
                && tokens.contains("NULL")) {

            column.setNullable(false);
        }

        if (tokens.contains("UNIQUE")) {

            column.setUnique(true);
        }

        parseReference(specs, column);
    }

    private void parseReference(
            List<String> specs,
            ColumnModel column) {

        for (int i = 0; i < specs.size(); i++) {

            if (!specs.get(i).equalsIgnoreCase("REFERENCES")) {
                continue;
            }

            column.setForeignKey(true);

            String reference = specs.get(i + 1);

            int start = reference.indexOf("(");
            int end = reference.indexOf(")");

            if (start > 0 && end > start) {

                column.setReferencedTable(
                        reference.substring(0, start)
                );

                column.setReferencedColumn(
                        reference.substring(start + 1, end)
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

                case "PRIMARY KEY" ->
                        parsePrimaryKey(index, table);

                case "FOREIGN KEY" ->
                        parseForeignKey(index, table);
            }
        }
    }

    private void parsePrimaryKey(
            Index index,
            TableModel table) {

        if (index.getColumnsNames() == null) {
            return;
        }

        for (String columnName :
                index.getColumnsNames()) {

            ColumnModel column =
                    getColumn(table, columnName);

            if (column != null) {

                column.setPrimaryKey(true);
                column.setNullable(false);
            }
        }
    }

    private void parseForeignKey(
            Index index,
            TableModel table) {

        if (index.getColumnsNames() == null
                || index.getColumnsNames().isEmpty()) {
            return;
        }

        ColumnModel column =
                getColumn(
                        table,
                        index.getColumnsNames().get(0)
                );

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
                                sql.toUpperCase().indexOf("REFERENCES")
                                        + "REFERENCES".length())
                        .trim();

        int start = reference.indexOf("(");
        int end = reference.indexOf(")");

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