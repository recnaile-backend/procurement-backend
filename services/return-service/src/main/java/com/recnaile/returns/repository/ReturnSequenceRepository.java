package com.recnaile.returns.repository;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class ReturnSequenceRepository {
    private final MongoTemplate mongoTemplate;
    public ReturnSequenceRepository(MongoTemplate mongoTemplate){this.mongoTemplate=mongoTemplate;}
    public long nextSequence(String fiscalYearKey){
        Query q=Query.query(Criteria.where("_id").is(fiscalYearKey));
        Update u=new Update().inc("sequence",1);
        ReturnSequenceDocument d=mongoTemplate.findAndModify(q,u,FindAndModifyOptions.options().upsert(true).returnNew(true),ReturnSequenceDocument.class);
        if(d==null) throw new IllegalStateException("Unable to allocate return number sequence");
        return d.getSequence();
    }
}
