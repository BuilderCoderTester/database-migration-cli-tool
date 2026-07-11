package com.project.demo.BeforeExecutionValidation.parser;

import com.project.demo.BeforeExecutionValidation.model.ColumnModel;
import com.project.demo.BeforeExecutionValidation.model.SchemaModel;
import com.project.demo.BeforeExecutionValidation.model.TableModel;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlterTableParser {

    public void parse(Alter alter, SchemaModel schema) {
        String tableName = alter.getTable().getName();

        TableModel table = schema.getTables().computeIfAbsent(
                tableName,
                t -> {
                    TableModel tm = new TableModel();
                    tm.setTableName(t);
                    return tm;
                });

        for (AlterExpression expression : alter.getAlterExpressions()) {
            AlterOperation operation = expression.getOperation();

            switch (operation) {
                case ADD -> {
                    // Route based on whether we are adding keys/constraints vs adding standard columns
                    if (expression.getPkColumns() != null && !expression.getPkColumns().isEmpty()) {
                        parsePrimaryKey(expression, table);
                    } else if (expression.getFkColumns() != null && !expression.getFkColumns().isEmpty()) {
                        parseForeignKey(expression, table);
                    } else {
                        parseAdd(expression, table);
                    }
                }
                case DROP -> parseDrop(expression, table);
                case MODIFY, ALTER, CHANGE -> parseModify(expression, table);
                case DROP_PRIMARY_KEY -> dropPrimaryKey(table);
                case DROP_FOREIGN_KEY -> dropForeignKey(expression, table);
                default -> {
                }
            }
        }
    }

    /**
     * ALTER TABLE ADD FOREIGN KEY
     */
    private void parseForeignKey(AlterExpression expression, TableModel table) {
        List<String> fkCols = expression.getFkColumns();
        if (fkCols == null || fkCols.isEmpty()) {
            return;
        }

        String columnName = fkCols.get(0);

        ColumnModel column = table.getColumns().computeIfAbsent(columnName, name -> {
            ColumnModel cm = new ColumnModel();
            cm.setColumnName(name);
            return cm;
        });

        column.setForeignKey(true);
        column.setReferencedTable(expression.getFkSourceTable());

        if (expression.getFkSourceColumns() != null && !expression.getFkSourceColumns().isEmpty()) {
            column.setReferencedColumn(expression.getFkSourceColumns().get(0));
        }
    }

    /**
     * ALTER TABLE DROP PRIMARY KEY
     */
    private void dropPrimaryKey(TableModel table) {
        table.getColumns()
                .values()
                .forEach(column -> column.setPrimaryKey(false));
    }

    /**
     * ALTER TABLE DROP FOREIGN KEY
     */
    private void dropForeignKey(AlterExpression expression, TableModel table) {
        List<String> columnsToDropFk = expression.getFkColumns();
        if (columnsToDropFk == null || columnsToDropFk.isEmpty()) {
            columnsToDropFk = expression.getPkColumns();
        }

        if (columnsToDropFk == null) {
            return;
        }

        for (String columnName : columnsToDropFk) {
            ColumnModel column = table.getColumns().get(columnName);
            if (column != null) {
                column.setForeignKey(false);
                column.setReferencedTable(null);
                column.setReferencedColumn(null);
            }
        }
    }

    /**
     * ALTER TABLE ADD COLUMN
     */
    private void parseAdd(AlterExpression expression, TableModel table) {
        if (expression.getColDataTypeList() == null) {
            return;
        }

        expression.getColDataTypeList().forEach(col -> {
            ColumnModel column = new ColumnModel();
            column.setColumnName(col.getColumnName());

            if (col.getColDataType() != null) {
                column.setDataType(col.getColDataType().toString());
            }

            if (col.getColumnSpecs() != null) {
                parseColumnSpecs(column, col.getColumnSpecs());
            }

            table.getColumns().put(column.getColumnName(), column);
        });
    }

    /**
     * ALTER TABLE DROP COLUMN
     */
    private void parseDrop(AlterExpression expression, TableModel table) {
        if (expression.getColumnName() != null) {
            table.getColumns().remove(expression.getColumnName());
        } else if (expression.getPkColumns() != null) {
            expression.getPkColumns().forEach(table.getColumns()::remove);
        }
    }

    private void parseColumnSpecs(ColumnModel column, java.util.List<String> specs) {
        var tokens = specs.stream()
                .map(String::toUpperCase)
                .toList();

        if (tokens.contains("PRIMARY") && tokens.contains("KEY")) {
            column.setPrimaryKey(true);
        }

        if (tokens.contains("NOT") && tokens.contains("NULL")) {
            column.setNullable(false);
        }

        if (tokens.contains("UNIQUE")) {
            column.setUnique(true);
        }

        for (int i = 0; i < specs.size(); i++) {
            if (!specs.get(i).equalsIgnoreCase("REFERENCES")) {
                continue;
            }

            column.setForeignKey(true);
            if (i + 1 < specs.size()) {
                String reference = specs.get(i + 1);

                int start = reference.indexOf("(");
                int end = reference.indexOf(")");

                if (start > 0 && end > start) {
                    column.setReferencedTable(reference.substring(0, start));
                    column.setReferencedColumn(reference.substring(start + 1, end));
                } else {
                    column.setReferencedTable(reference);
                }
            }
        }
    }

    /**
     * MODIFY COLUMN / CHANGE COLUMN
     */
    private void parseModify(AlterExpression expression, TableModel table) {
        if (expression.getColDataTypeList() == null) {
            return;
        }

        expression.getColDataTypeList().forEach(col -> {
            ColumnModel existing = table.getColumns().get(col.getColumnName());

            if (existing == null) {
                existing = new ColumnModel();
                existing.setColumnName(col.getColumnName());
                table.getColumns().put(existing.getColumnName(), existing);
            }

            if (col.getColDataType() != null) {
                existing.setDataType(col.getColDataType().toString());
            }

            if (col.getColumnSpecs() != null) {
                parseColumnSpecs(existing, col.getColumnSpecs());
            }
        });
    }

    /**
     * ALTER TABLE ADD PRIMARY KEY
     */
    private void parsePrimaryKey(AlterExpression expression, TableModel table) {
        List<String> columns = expression.getPkColumns();
        if (columns == null) {
            return;
        }

        for (String columnName : columns) {
            ColumnModel column = table.getColumns().computeIfAbsent(columnName, name -> {
                ColumnModel cm = new ColumnModel();
                cm.setColumnName(name);
                return cm;
            });

            column.setPrimaryKey(true);
            column.setNullable(false);
        }
    }
}