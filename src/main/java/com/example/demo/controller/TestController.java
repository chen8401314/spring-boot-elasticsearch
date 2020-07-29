/**
 * Copyright (C), 2015-2019, 华规软件（上海）有限公司
 * FileName: CepController
 * Author:   shenqicheng
 * Date:     2020/3/16 17:17
 * Description:
 * History:
 */
package com.example.demo.controller;

import com.example.demo.common.Response;
import com.example.demo.entity.TestEntity;
import com.example.demo.repo.TestRepo;
import com.example.demo.util.SecurityUtil;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

/**
 * @author shenqicheng
 * @Date: 2020/3/16 17:17
 * @Description: 主要用于迁移api原有对接第三方平台接口
 * cep 文件获取和网址获取(文件相关是否可以结合minIo整合)
 */
@Slf4j
@Validated
@RestController("TestController")
@RequestMapping(value = "/test", produces = {APPLICATION_JSON_UTF8_VALUE})
@Api(tags = "test", produces = MediaType.ALL_VALUE)
public class TestController {

    @Autowired
    TestRepo testRepo;
    @Autowired
    ElasticsearchTemplate elsTemplate;

    @GetMapping(value = "/addData")
    @ApiOperation(value = "添加数据")
    public Response<String> addData(int num) {
        List<TestEntity> list = Lists.newArrayList();
        for (int i = 0; i < num; i++) {
            TestEntity testEntity = new TestEntity();
            testEntity.setId(SecurityUtil.getUUID());
            testEntity.setCreatedTime(new Date());
            testEntity.setName("name" + new Random().nextInt(10000));
            testEntity.setAddress("address" + new Random().nextInt(10000));
            list.add(testEntity);
        }
        testRepo.saveAll(list);
        return Response.success();
    }


    @DeleteMapping(value = "/delById")
    @ApiOperation(value = "删除数据")
    public Response<String> delById(String id) {
        testRepo.deleteById(id);
        return Response.success();
    }


    @DeleteMapping(value = "/delIndex")
    @ApiOperation(value = "删除索引")
    public Response<String> delIndex(String indexName) {
        elsTemplate.deleteIndex(indexName);
        return Response.success();
    }

    @GetMapping(value = "/query")
    @ApiOperation(value = "查询")
    public Response<Page<TestEntity>> query(String name, int page, int size) {
        QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.wildcardQuery("name.keyword", name));
        //Page<EsEntity> result = esRep.search(queryBuilder, PageRequest.of(page-1, size));
        SortBuilder sort = SortBuilders.fieldSort("createdTime").order(SortOrder.DESC);
        SearchQuery searchQuery = new NativeSearchQueryBuilder()//构建查询对象
                .withQuery(queryBuilder)
                .withSort(sort)
                .withPageable(PageRequest.of(page - 1, size))//分页
                .build();
        Page<TestEntity> result = testRepo.search(searchQuery);
        return Response.success(result);
    }

}
