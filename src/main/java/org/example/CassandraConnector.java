package org.example;

import com.datastax.oss.driver.api.core.CqlSession;

import java.net.InetSocketAddress;

public class CassandraConnector {
    public static CqlSession getSession() {
        System.out.println(" CassandraConnector: connecting to cassandra:9042");

        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress("cassandra", 9042))
                .withLocalDatacenter("datacenter1")
                .withKeyspace("student_keyspace")
                .build();
    }
}
