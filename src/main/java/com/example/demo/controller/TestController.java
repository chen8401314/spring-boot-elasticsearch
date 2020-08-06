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
import com.example.demo.dto.ResultDTO;
import com.example.demo.entity.TestEntity;
import com.example.demo.repo.TestRepo;
import com.example.demo.request.SearchReq;
import com.example.demo.util.SecurityUtil;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
    @Autowired
    Client client;

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
        QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.wildcardQuery("name", name));
        SortBuilder sort1 = SortBuilders.fieldSort("createdTime").order(SortOrder.DESC);
        SortBuilder sort2 = SortBuilders.fieldSort("id").order(SortOrder.ASC);
        SearchQuery searchQuery = new NativeSearchQueryBuilder()//构建查询对象
                .withQuery(queryBuilder)
                .withSort(sort1)
                .withSort(sort2)
                .withPageable(PageRequest.of(page - 1, size))//分页
                .build();
        Page<TestEntity> result = testRepo.search(searchQuery);
        return Response.success(result);
    }


    @PostMapping(value = "/queryAfter")
    @ApiOperation(value = "深度分页查询")
    public Response<ResultDTO> queryAfter(@RequestBody SearchReq searchReq) {
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //name模糊匹配
        searchSourceBuilder.query(QueryBuilders.wildcardQuery("name", searchReq.getName()));
        //第一次查询dsorts传空
        if (searchReq.getSorts() != null && searchReq.getSorts().length != 0) {
            searchSourceBuilder.searchAfter(searchReq.getSorts());
        }
        //每页显示数
        searchSourceBuilder.size(10);
        //排序
        searchSourceBuilder.sort("createdTime", SortOrder.DESC);
        searchSourceBuilder.sort("id", SortOrder.ASC);
        searchRequest.source(searchSourceBuilder);
        //指定index和type
        searchRequest.indices("test10_doc").types("doc").source(searchSourceBuilder);
        SearchResponse sr = client.search(searchRequest).actionGet();

        SearchHits hits = sr.getHits();
        SearchHit[] searchHits = hits.getHits();
        List<Map<String, Object>> list = Lists.newArrayList();
        ResultDTO resultDTO = new ResultDTO();
        //设置总数
        resultDTO.setTotalCount(hits.getTotalHits());
        //遍历查询结果
        for (SearchHit hit : searchHits) {
            //取_source字段值
            String sourceAsString = hit.getSourceAsString(); //取成json串
            System.out.println(sourceAsString);
            Map<String, Object> sourceAsMap = hit.getSourceAsMap(); // 取成map对象
            list.add(sourceAsMap);
            resultDTO.setSorts(hit.getSortValues());
        }
        resultDTO.setContent(list);
        return Response.success(resultDTO);
    }

}
