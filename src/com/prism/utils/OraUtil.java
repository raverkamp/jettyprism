package com.prism.utils;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import oracle.jdbc.OracleConnection;

public class OraUtil {

    public static class ResolvedProcedure {

        public final String owner;
        public final String package_;
        public final String procedure;
        public final String fullName;

        public ResolvedProcedure(String owner,
                String package_,
                String procedure) {
            this.owner = owner;
            this.package_ = package_;
            this.procedure = procedure;
            if (this.package_ == null) {
                this.fullName = this.owner + "." + this.procedure;
            } else {
                this.fullName = this.owner + "." + this.package_ + "." + this.procedure;
            }
        }

    }

    public static ResolvedProcedure resolveProcedure(OracleConnection sqlconn, String procname) throws SQLException {
        try (CallableStatement css = sqlconn.prepareCall("BEGIN \n dbms_utility.name_resolve(?,1,?,?,?,?,?,?); \nEND;")) {
            css.setString(1, procname);
            css.registerOutParameter(2, Types.VARCHAR);
            css.registerOutParameter(3, Types.VARCHAR);
            css.registerOutParameter(4, Types.VARCHAR);
            css.registerOutParameter(5, Types.VARCHAR);
            css.registerOutParameter(6, Types.VARCHAR);
            css.registerOutParameter(7, Types.VARCHAR);
            try {
                css.execute();
            } catch (SQLException e) {

                if (e.getErrorCode() == 6564) {
                    return null;
                }
            }
            String owner = css.getString(2);
            String package_ = css.getString(3);
            String procedure = css.getString(4);
            return new ResolvedProcedure(owner, package_, procedure);
        }
    }
}
