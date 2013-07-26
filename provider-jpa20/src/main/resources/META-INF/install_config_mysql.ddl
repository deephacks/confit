-- CREATE TABLES
create table CONFIG_BEAN (
  BEAN_ID varchar(40) not null, 
  BEAN_SCHEMA_NAME varchar(60) not null, 
  primary key (BEAN_ID, BEAN_SCHEMA_NAME)
) ENGINE=InnoDB;

create table CONFIG_BEAN_SINGLETON (
  BEAN_SCHEMA_NAME varchar(60) not null, 
  primary key (BEAN_SCHEMA_NAME)
) ENGINE=InnoDB;

create table CONFIG_BEAN_REF (
  UUID varchar(40) not null, 
  FK_SOURCE_BEAN_SCHEMA_NAME varchar(60) not null, 
  FK_SOURCE_BEAN_ID varchar(40) not null, 
  PROP_NAME varchar(40) not null, 
  FK_TARGET_BEAN_ID varchar(40) not null, 
  FK_TARGET_BEAN_SCHEMA_NAME varchar(60) not null, 
  primary key (UUID)
) ENGINE=InnoDB;

create table CONFIG_PROPERTY (
  UUID varchar(40) not null, 
  FK_BEAN_ID varchar(40) not null, 
  PROP_NAME varchar(40) not null, 
  FK_BEAN_SCHEMA_NAME varchar(60) not null, 
  PROP_VALUE varchar(255), 
  primary key (UUID)
) ENGINE=InnoDB;

-- FOREIGN KEY CONSTRAINTS
alter table CONFIG_BEAN_REF
  add constraint FK_CONFIG_REF_BEAN 
    foreign key (FK_SOURCE_BEAN_ID, FK_SOURCE_BEAN_SCHEMA_NAME)
      references CONFIG_BEAN (BEAN_ID, BEAN_SCHEMA_NAME) ON DELETE RESTRICT;
      
alter table CONFIG_BEAN_REF
  add constraint FK_CONFIG_REF_FK_BEAN
    foreign key (FK_TARGET_BEAN_ID, FK_TARGET_BEAN_SCHEMA_NAME)
      references CONFIG_BEAN (BEAN_ID, BEAN_SCHEMA_NAME) ON DELETE RESTRICT;
      
alter table CONFIG_PROPERTY 
  add constraint FK_CONFIG_BEAN_PROPERTY 
    foreign key (FK_BEAN_ID, FK_BEAN_SCHEMA_NAME) 
      references CONFIG_BEAN (BEAN_ID, BEAN_SCHEMA_NAME);
