//package com.fergie.lab1.config;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//import jakarta.persistence.EntityManagerFactory;
//import javax.sql.DataSource;
//import org.eclipse.persistence.jpa.PersistenceProvider;
//@Configuration
//@EnableTransactionManagement
//public class EclipseLinkConfig {
//    @Bean
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
//        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
//        emf.setDataSource(dataSource);
//        emf.setPackagesToScan("com.fergie.lab1.models");
//        emf.setPersistenceProviderClass(PersistenceProvider.class);
//        return emf;
//    }
//    @Bean
//    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
//        return new JpaTransactionManager(entityManagerFactory);
//    }
//}