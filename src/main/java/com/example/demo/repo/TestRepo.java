package com.example.demo.repo;

import com.example.demo.entity.TestEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TestRepo extends ElasticsearchRepository<TestEntity, String> {
}
