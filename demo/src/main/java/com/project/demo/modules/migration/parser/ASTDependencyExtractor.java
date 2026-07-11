package com.project.demo.modules.migration.parser;

import com.project.demo.enumuration.AlterOperation;
import com.project.demo.enumuration.DatabaseOperation;
import com.project.demo.enumuration.DependencyType;
import com.project.demo.modules.migration.dto.dependency.response.DependencyAnalysisResult;
import com.project.demo.modules.migration.dto.schemaRequest.ColumnSchemaDto;
import com.project.demo.modules.migration.model.Dependency;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ASTDependencyExtractor {

    public DependencyAnalysisResult extract(String sql) throws Exception {

        Statement stmt = CCJSqlParserUtil.parse(sql);
        List<Dependency> deps = new ArrayList<>();
        DatabaseOperation operation = null;
        AlterOperation alterOperation = null;
        if (stmt instanceof CreateTable createTable) {
            System.out.println("in the create table operation");
            operation = DatabaseOperation.CREATE;
            extractCreateTable(createTable, deps);

        }

        else if (stmt instanceof Select select) {
            operation = DatabaseOperation.SELECT;
            extractFromSelect(select, deps);
        }

        else if (stmt instanceof Insert insert) {
            operation = DatabaseOperation.INSERT;
            extractInsert(insert, deps);
        }

        else if (stmt instanceof Update update) {
            operation = DatabaseOperation.UPDATE;
            extractUpdate(update, deps);
        }

        else if (stmt instanceof Delete delete) {
            operation = DatabaseOperation.DELETE;
            extractDelete(delete, deps);
        }

        else if (stmt instanceof Alter alter) {

            operation = DatabaseOperation.ALTER;
            alterOperation = extractAlter(alter, deps);
        }


        else if (stmt instanceof Drop drop) {

            operation = DatabaseOperation.DROP;
            extractDrop(drop, deps);
        }
        else if (stmt instanceof CreateIndex createIndex) {

            operation = DatabaseOperation.CREATE;

            extractCreateIndex(createIndex, deps);
        }
        else {

            throw new UnsupportedOperationException(
                    "Unsupported SQL statement: " + stmt.getClass().getSimpleName()
            );
        }
        return new DependencyAnalysisResult(
                operation,
                alterOperation,
                deps
        );
    }

    /*------------------------------------------------------*/
    /* CREATE TABLE                                         */
    /*------------------------------------------------------*/
    private void extractCreateIndex(
            CreateIndex createIndex,
            List<Dependency> deps) {

        String table = createIndex.getTable().getName();

        // Create INDEX dependency
        Dependency dependency = new Dependency();
        dependency.setType(DependencyType.INDEX);
        dependency.setTable(table);
        dependency.setIndexName(createIndex.getIndex().getName()); // <-- Store the index name
        deps.add(dependency);

        // Create COLUMN dependencies
        if (createIndex.getIndex() != null &&
                createIndex.getIndex().getColumnsNames() != null) {

            createIndex.getIndex().getColumnsNames().forEach(column -> {

                ColumnSchemaDto dto = new ColumnSchemaDto();
                dto.setColumnName(column);

                deps.add(new Dependency(
                        DependencyType.COLUMN,
                        table,
                        dto,
                        null,
                        null
                ));
            });
        }
    }
    private void extractCreateTable(CreateTable createTable, List<Dependency> deps) {
        if (createTable.getTable() == null) return;

        String tableName = createTable.getTable().getName();

        deps.add(new Dependency(
                DependencyType.TABLE,
                tableName,
                null,
                null,
                null
        ));

        System.out.println("\n[AST Extractor] Parse Action: CREATE TABLE");
        System.out.println(" └─ 📋 Table: " + tableName);

        if (createTable.getColumnDefinitions() != null) {
            int colCount = createTable.getColumnDefinitions().size();
            System.out.println(" └─ 🗂️ Processing " + colCount + " columns...");

            createTable.getColumnDefinitions().forEach(def -> {
                ColumnSchemaDto dto = new ColumnSchemaDto();
                dto.setColumnName(def.getColumnName());

                deps.add(new Dependency(
                        DependencyType.COLUMN,
                        tableName,
                        dto,
                        null,
                        null
                ));

                System.out.println("     ├── Column Found: " + def.getColumnName());
            });
        }
        System.out.println("──────────────────────────────────────────────────");
    }

    /*------------------------------------------------------*/
    /* INSERT                                               */
    /*------------------------------------------------------*/

    private void extractInsert(
            Insert insert,
            List<Dependency> deps) {

        String table = insert.getTable().getName();

        deps.add(new Dependency(
                DependencyType.INSERT,
                table,
                null,
                null,
                null
        ));

        if (insert.getColumns() != null) {

            insert.getColumns().forEach(column -> {

                ColumnSchemaDto dto = new ColumnSchemaDto();
                dto.setColumnName(column.getColumnName());

                deps.add(new Dependency(
                        DependencyType.COLUMN,
                        table,
                        dto,
                        null,
                        null
                ));
            });
        }
    }

    /*------------------------------------------------------*/
    /* UPDATE                                               */
    /*------------------------------------------------------*/

    private void extractUpdate(
            Update update,
            List<Dependency> deps) {

        String table = update.getTable().getName();

        deps.add(new Dependency(
                DependencyType.UPDATE,
                table,
                null,
                null,
                null
        ));

        update.getColumns().forEach(column -> {

            ColumnSchemaDto dto = new ColumnSchemaDto();
            dto.setColumnName(column.getColumnName());

            deps.add(new Dependency(
                    DependencyType.COLUMN,
                    table,
                    dto,
                    null,
                    null
            ));
        });
    }

    /*------------------------------------------------------*/
    /* DELETE                                               */
    /*------------------------------------------------------*/

    private void extractDelete(
            Delete delete,
            List<Dependency> deps) {

        deps.add(new Dependency(
                DependencyType.DELETE,
                delete.getTable().getName(),
                null,
                null,
                null
        ));
    }

    /*------------------------------------------------------*/
    /* ALTER                                                */
    /*------------------------------------------------------*/

    private AlterOperation extractAlter(
            Alter alter,
            List<Dependency> deps) {

        deps.add(new Dependency(
                DependencyType.ALTER,
                alter.getTable().getName(),
                null,
                null,
                null
        ));

        AlterOperation alterOperation = null;

        if (alter.getAlterExpressions() != null) {

            for (AlterExpression exp : alter.getAlterExpressions()) {

                System.out.println("ALTER OPERATION = " + exp.getOperation());
                System.out.println("COLUMN = " + exp.getColumnName());
                System.out.println("EXPRESSION = " + exp);

                Dependency dependency = new Dependency();
                dependency.setType(DependencyType.COLUMN);
                dependency.setTable(alter.getTable().getName());

                switch (exp.getOperation()) {

                    case ADD:

                        alterOperation = AlterOperation.ADD_COLUMN;

                        if (exp.getColumnName() != null) {
                            ColumnSchemaDto dto = new ColumnSchemaDto();
                            dto.setColumnName(exp.getColumnName());
                            dependency.setColumn(dto);
                        }

                        break;

                    case DROP:

                        alterOperation = AlterOperation.DROP_COLUMN;

                        if (exp.getColumnName() != null) {
                            ColumnSchemaDto dto = new ColumnSchemaDto();
                            dto.setColumnName(exp.getColumnName());
                            dependency.setColumn(dto);
                        }

                        break;

                    case MODIFY:
                    case ALTER:

                        alterOperation = AlterOperation.MODIFY_COLUMN;

                        if (exp.getColumnName() != null) {
                            ColumnSchemaDto dto = new ColumnSchemaDto();
                            dto.setColumnName(exp.getColumnName());
                            dependency.setColumn(dto);
                        }

                        break;


                    case RENAME:

                        alterOperation = AlterOperation.RENAME_COLUMN;

                        ColumnSchemaDto oldColumn = new ColumnSchemaDto();
                        oldColumn.setColumnName(exp.getColumnOldName());

                        ColumnSchemaDto newColumn = new ColumnSchemaDto();
                        newColumn.setColumnName(exp.getColumnName());

                        dependency.setColumn(oldColumn);
                        dependency.setTargetColumn(newColumn);

                        break;
                }

                deps.add(dependency);
            }
        }

        return alterOperation;
    }

    /*------------------------------------------------------*/
    /* DROP                                                 */
    /*------------------------------------------------------*/

    private void extractDrop(
            Drop drop,
            List<Dependency> deps) {

        deps.add(new Dependency(
                DependencyType.DROP,
                drop.getName().getName(),
                null,
                null,
                null
        ));
    }

    /*------------------------------------------------------*/
    /* SELECT                                               */
    /*------------------------------------------------------*/

    private void extractFromSelect(
            Select select,
            List<Dependency> deps) {

        select.getSelectBody().accept(new SelectVisitorAdapter() {

            @Override
            public void visit(PlainSelect plainSelect) {

                if (plainSelect.getFromItem() instanceof Table table) {

                    deps.add(new Dependency(
                            DependencyType.TABLE,
                            table.getName(),
                            null,
                            null,
                            null
                    ));
                }

                if (plainSelect.getJoins() != null) {

                    plainSelect.getJoins().forEach(join -> {

                        if (join.getRightItem() instanceof Table table) {

                            deps.add(new Dependency(
                                    DependencyType.TABLE,
                                    table.getName(),
                                    null,
                                    null,
                                    null
                            ));
                        }
                    });
                }
            }
        });
    }
}