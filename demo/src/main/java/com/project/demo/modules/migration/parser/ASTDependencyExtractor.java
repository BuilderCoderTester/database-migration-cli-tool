package com.project.demo.modules.migration.parser;

import com.project.demo.enumuration.DependencyType;
import com.project.demo.modules.migration.model.Dependency;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ASTDependencyExtractor {

    public List<Dependency> extract(String sql) throws Exception {
        Statement stmt = CCJSqlParserUtil.parse(sql);
        List<Dependency> deps = new ArrayList<>();

        if (stmt instanceof CreateTable createTable) {

            if (createTable.getIndexes() != null) {

                createTable.getIndexes().forEach(index -> {

                    if ("FOREIGN KEY".equalsIgnoreCase(index.getType())) {

                        String fk = index.toString();

                        // Example:
                        // FOREIGN KEY (user_id) REFERENCES users(id)

                        if (fk.contains("REFERENCES")) {

                            String refPart = fk.split("REFERENCES")[1].trim();

                            String refTable = refPart.substring(0, refPart.indexOf("(")).trim();

//                            deps.add(new Dependency(
//                                    DependencyType.TABLE,
//                                    refTable,
//                                    null,
//                                    null,
//                                    null
//                            ));
                        }
                    }
                });
            }
        }

        if (stmt instanceof Select select) {
            extractFromSelect(select, deps);
        }

        if (stmt instanceof Insert insert) {
            Table table = insert.getTable();
            deps.add(new Dependency(DependencyType.TABLE, table.getName(), null, null, null));
        }

        if (stmt instanceof Update update) {
            Table table = update.getTable();
            deps.add(new Dependency(DependencyType.TABLE, table.getName(), null, null, null));

            update.getColumns().forEach(col ->
                    deps.add(new Dependency(DependencyType.COLUMN, table.getName(), col.getColumnName(), null, null))
            );
        }

        if (stmt instanceof Alter alter) {
            Table table = alter.getTable();
            deps.add(new Dependency(DependencyType.TABLE, table.getName(), null, null, null));
        }

        return deps;
    }

    private void extractFromSelect(Select select, List<Dependency> deps) {
        select.getSelectBody().accept(new SelectVisitorAdapter() {

            @Override
            public void visit(PlainSelect plainSelect) {

                // FROM table
                if (plainSelect.getFromItem() instanceof Table table) {
                    deps.add(new Dependency(DependencyType.TABLE, table.getName(), null, null, null));
                }

                // JOIN tables
                if (plainSelect.getJoins() != null) {
                    plainSelect.getJoins().forEach(join -> {
                        if (join.getRightItem() instanceof Table table) {
                            deps.add(new Dependency(DependencyType.TABLE, table.getName(), null, null, null));
                        }
                    });
                }
            }
        });
    }
}