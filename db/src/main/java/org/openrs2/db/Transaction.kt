package org.openrs2.db

import java.sql.Connection

/**
 * A functional interface representing a single database transation.
 * @param T the result type. Use [Unit] if the transaction does not return a
 * result.
 */
public fun interface Transaction<T> {
    /**
     * Executes the transaction on the given [connection]. It is not necessary
     * to implement commit or rollback logic yourself.
     *
     * The transaction may be called multiple times if a deadlock occurs, so
     * care needs to be taken if the transaction has any application-level side
     * effects.
     * @param connection the database connection, which is only valid for the
     * duration of the transaction.
     * @return the result.
     */
    public fun execute(connection: Connection): T
}
