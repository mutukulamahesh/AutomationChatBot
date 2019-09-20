
DROP DATABASE chatbot;

CREATE DATABASE chatbot;

USE chatbot;

CREATE TABLE nodes (
    id int NOT NULL AUTO_INCREMENT,
    node varchar(255) NOT NULL,
    allow_custom boolean,
    PRIMARY KEY (id)
);

CREATE INDEX idx_nodes ON nodes (id);

CREATE TABLE relations (
    id int NOT NULL AUTO_INCREMENT,
    parent_node_id int NOT NULL,
    child_node_id int NOT NULL,
    vocabulary_id int NOT NULL,
    had_next_relations int NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE vocabulary (
    id int NOT NULL AUTO_INCREMENT,
    message TEXT NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO nodes (node,allow_custom) VALUES ('Bat',0), ('Sanity',0), ('Cloud',1), ('V5',1), ('1.2',1), ('VAB',1), ('LP',1), ('Teams',1), ('IRMA',0), ('CIRUS',0), ('TEAM-SWAT',0), ('ARROWS',0), ('CB',0), ('Auto-Prop',0), ('SK003',0);

INSERT INTO relations (parent_node_id, child_node_id, vocabulary_id, had_next_relations) VALUES (0,1,4,0), (0,2,4,0), (1,3,4,0), (1,4,4,0), (1,5,4,0), (2,3,4,0), (2,4,4,0), (2,5,4,0), (3,6,4,0), (3,7,4,0), (3,8,4,0), (4,6,4,0), (4,7,4,0), (4,8,4,0),(5,6,4,0), (5,7,4,0), (5,8,4,0), (6,14,4,0), (6,15,4,0), (7,14,4,0), (7,15,4,0), (8,9,4,0), (8,10,4,0), (8,11,4,0), (8,12,4,0), (8,13,4,0), (9,14,4,0), (9,15,4,0), (10,14,4,0), (10,15,4,0), (11,14,4,0), (11,15,4,0), (12,14,4,0), (12,15,4,0), (13,14,4,0), (13,15,4,0);

INSERT INTO vocabulary (message) VALUES
("Hello <<name>>!!, How are you? This is ECLBot, I will help you to know the Apple products price. Which Apple product price you want to get to know? is that <<relations>>?"),
("That is interesting <<name>>!! which series <<node>> are you looking for? is that <<relations>>?"),
("Great decission <<name>>, by the way which model <<node>> whould you like to enquire about? Is that <<relations>>"),
("The price of <<node>> is : <<relations>>"),
("Thanks you <<name>> for contacting ECLBot. Its a wonderful chat!! would you mind giving me a feedback?"),
("Thank you so much <<name>>, See you again!! Bye");

