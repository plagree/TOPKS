--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: docs; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--
--created during processing

CREATE TABLE docs (
    item text,
    tag text,
    num bigint
);


ALTER TABLE public.docs OWNER TO postgres;

--
-- Name: hist_soc_snet_d; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE TABLE hist_soc_snet_d (
--    "user" integer,
--    func text,
--    bucket numeric,
--    num numeric
--);
--
--
--ALTER TABLE public.hist_soc_snet_d OWNER TO postgres;
--
----
---- Name: hist_soc_snet_dt; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE TABLE hist_soc_snet_dt (
--    "user" integer,
--    func text,
--    bucket numeric,
--    num numeric
--);
--
--
--ALTER TABLE public.hist_soc_snet_dt OWNER TO postgres;

--
-- Name: hist_soc_snet_tt; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE hist_soc_snet_tt (
    "user" integer,
    func text,
    bucket numeric,
    num numeric
);


ALTER TABLE public.hist_soc_snet_tt OWNER TO postgres;

--
-- Name: landmarks_soc_snet_d; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE TABLE landmarks_soc_snet_d (
--    func text,
--    landmark integer,
--    "user" integer,
--    dist numeric
--);
--
--
--ALTER TABLE public.landmarks_soc_snet_d OWNER TO postgres;
--
----
---- Name: landmarks_soc_snet_dt; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE TABLE landmarks_soc_snet_dt (
--    func text,
--    landmark integer,
--    "user" integer,
--    dist numeric
--);
--
--
--ALTER TABLE public.landmarks_soc_snet_dt OWNER TO postgres;

--
-- Name: landmarks_soc_snet_tt; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE landmarks_soc_snet_tt (
    func text,
    landmark integer,
    "user" integer,
    dist numeric
);


ALTER TABLE public.landmarks_soc_snet_tt OWNER TO postgres;

--
-- Name: sample; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE sample (
    "user" integer
);


ALTER TABLE public.sample OWNER TO postgres;

--
-- Name: seekers; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE seekers (
    seeker bigint,
    score numeric
);


ALTER TABLE public.seekers OWNER TO postgres;

--
-- Name: soc_snet_d; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE TABLE soc_snet_d (
--    user1 integer,
--    user2 integer,
--    weight numeric
--);
--
--
--ALTER TABLE public.soc_snet_d OWNER TO postgres;
--
----
---- Name: soc_snet_d_bak; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE TABLE soc_snet_d_bak (
--    user1 integer,
--    user2 integer,
--    weight numeric
--);
--
--
--ALTER TABLE public.soc_snet_d_bak OWNER TO postgres;
--
----
---- Name: soc_snet_dt; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE TABLE soc_snet_dt (
--    user1 integer,
--    user2 integer,
--    weight numeric
--);
--
--
--ALTER TABLE public.soc_snet_dt OWNER TO postgres;
--
----
---- Name: soc_snet_dt_bak; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE TABLE soc_snet_dt_bak (
--    user1 integer,
--    user2 integer,
--    weight numeric
--);
--
--
--ALTER TABLE public.soc_snet_dt_bak OWNER TO postgres;
--
----
---- Name: soc_snet_tt; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
----

CREATE TABLE soc_snet_tt (
    user1 integer,
    user2 integer,
    weight numeric
);


ALTER TABLE public.soc_snet_tt OWNER TO postgres;

--
-- Name: soc_snet_tt_bak; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE TABLE soc_snet_tt_bak (
--    user1 integer,
--    user2 integer,
--    weight numeric
--);
--
--
--ALTER TABLE public.soc_snet_tt_bak OWNER TO postgres;

--
-- Name: tagging; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE tagging (
    "user" integer,
    item text,
    tag text
);


ALTER TABLE public.tagging OWNER TO postgres;

--
-- Name: soc_tag_80_bak; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE TABLE soc_tag_80_bak (
--    "user" integer,
--    item text,
--    tag text
--);
--
--
--ALTER TABLE public.soc_tag_80_bak OWNER TO postgres;

--
-- Name: stats_soc_snet_d; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE TABLE stats_soc_snet_d (
--    "user" integer,
--    func text,
--    mean numeric,
--    var numeric
--);
--
--
--ALTER TABLE public.stats_soc_snet_d OWNER TO postgres;
--
----
---- Name: stats_soc_snet_dt; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE TABLE stats_soc_snet_dt (
--    "user" integer,
--    func text,
--    mean numeric,
--    var numeric
--);
--
--
--ALTER TABLE public.stats_soc_snet_dt OWNER TO postgres;

--
-- Name: stats_soc_snet_tt; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE stats_soc_snet_tt (
    "user" integer,
    func text,
    mean numeric,
    var numeric
);


ALTER TABLE public.stats_soc_snet_tt OWNER TO postgres;

--
-- Name: status; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE status (
    users bigint,
    items bigint,
    tags bigint
);


ALTER TABLE public.status OWNER TO postgres;

--
-- Name: tagfreq; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--
--created during processing

CREATE TABLE tagfreq (
    tag text,
    num bigint
);


ALTER TABLE public.tagfreq OWNER TO postgres;

--
-- Name: twitter_all; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE TABLE twitter_all (
--    "user" integer,
--    item text,
--    tag text
--);
--
--
--ALTER TABLE public.twitter_all OWNER TO postgres;

--
-- Name: view_items; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE view_items (
    id integer NOT NULL,
    qid numeric,
    item text,
    wscore numeric,
    bscore numeric
);


ALTER TABLE public.view_items OWNER TO postgres;

--
-- Name: view_keywords; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE view_keywords (
    id integer NOT NULL,
    qid numeric,
    tag text
);


ALTER TABLE public.view_keywords OWNER TO postgres;

--
-- Name: view_queries; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE view_queries (
    qid integer NOT NULL,
    seeker numeric,
    alpha numeric,
    func text,
    scfunc text,
    taggers text,
    network text,
    hidden numeric,
    coeff numeric
);


ALTER TABLE public.view_queries OWNER TO postgres;

--
-- Name: view_items_td_pk; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY view_items
    ADD CONSTRAINT view_items_td_pk PRIMARY KEY (id);


--
-- Name: view_keywords_td_pk; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY view_keywords
    ADD CONSTRAINT view_keywords_td_pk PRIMARY KEY (id);


--
-- Name: view_queries_td_pk; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY view_queries
    ADD CONSTRAINT view_queries_td_pk PRIMARY KEY (qid);


--
-- Name: idx_docs_item; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_docs_item ON docs USING btree (item);


--
-- Name: idx_docs_tag; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_docs_tag ON docs USING btree (tag);


--
-- Name: idx_dt_user1; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE INDEX idx_dt_user1 ON soc_snet_dt USING btree (user1);
--
--
----
---- Name: idx_h_d_func; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE INDEX idx_h_d_func ON hist_soc_snet_d USING btree (func);
--
--
----
---- Name: idx_h_d_user; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE INDEX idx_h_d_user ON hist_soc_snet_d USING btree ("user");
--
--
----
---- Name: idx_h_dt_func; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE INDEX idx_h_dt_func ON hist_soc_snet_dt USING btree (func);
--
--
----
---- Name: idx_h_dt_user; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE INDEX idx_h_dt_user ON hist_soc_snet_dt USING btree ("user");


--
-- Name: idx_h_tt_func; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_h_tt_func ON hist_soc_snet_tt USING btree (func);


--
-- Name: idx_h_tt_user; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_h_tt_user ON hist_soc_snet_tt USING btree ("user");


--
-- Name: idx_l_dt_func; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE INDEX idx_l_dt_func ON landmarks_soc_snet_dt USING btree (func);


--
-- Name: idx_l_tt_func; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_l_tt_func ON landmarks_soc_snet_tt USING btree (func);


--
-- Name: idx_land_b; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE INDEX idx_land_b ON landmarks_soc_snet_d USING btree (func);


--
-- Name: idx_s_d_func; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

--CREATE INDEX idx_s_d_func ON stats_soc_snet_d USING btree (func);
--
--
----
---- Name: idx_s_d_user; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE INDEX idx_s_d_user ON stats_soc_snet_d USING btree ("user");
--
--
----
---- Name: idx_s_d_user1; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE INDEX idx_s_d_user1 ON soc_snet_d USING btree (user1);
--
--
----
---- Name: idx_s_dt_func; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE INDEX idx_s_dt_func ON stats_soc_snet_dt USING btree (func);
--
--
----
---- Name: idx_s_dt_user; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
----
--
--CREATE INDEX idx_s_dt_user ON stats_soc_snet_dt USING btree ("user");


--
-- Name: idx_s_tt_func; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_s_tt_func ON stats_soc_snet_tt USING btree (func);


--
-- Name: idx_s_tt_user; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_s_tt_user ON stats_soc_snet_tt USING btree ("user");


--
-- Name: idx_tagging_item; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_tagging_item ON tagging USING btree (item);


--
-- Name: idx_tagging_tag; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_tagging_tag ON tagging USING btree (tag);


--
-- Name: idx_tagging_user; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_tagging_user ON tagging USING btree ("user");


--
-- Name: idx_tf_tag; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_tf_tag ON tagfreq USING btree (tag);


--
-- Name: idx_tt_user1; Type: INDEX; Schema: public; Owner: postgres; Tablespace: 
--

CREATE INDEX idx_tt_user1 ON soc_snet_tt USING btree (user1);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

