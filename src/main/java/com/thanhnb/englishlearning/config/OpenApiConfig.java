package com.thanhnb.englishlearning.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                    .title("English Learning API")
                    .version("1.0")
                    .description("API documentation for English Learning Application"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                    // Security Schemes
                    .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your JWT token in the format: Bearer <token>"))
                    
                    // Page Schemas cho Swagger UI
                    .addSchemas("PageGrammarTopicDTO", createPageSchema("GrammarTopicDTO"))
                    .addSchemas("PageGrammarLessonDTO", createPageSchema("GrammarLessonDTO"))
                    .addSchemas("PageGrammarQuestionDTO", createPageSchema("GrammarQuestionDTO"))
                    .addSchemas("Pageable", createPageableSchema())
                    .addSchemas("Sort", createSortSchema())
                );
    }

    /**
     * Tạo schema cho Page<T> - Spring Data structure
     */
    private Schema<?> createPageSchema(String dtoName) {
        Schema<?> pageSchema = new ObjectSchema();
        
        // content: Array of DTOs
        pageSchema.addProperty("content", new ArraySchema()
            .items(new Schema<>().$ref("#/components/schemas/" + dtoName))
            .description("Danh sách items trong trang hiện tại"));
        
        // pageable: Thông tin về request pagination
        pageSchema.addProperty("pageable", new Schema<>()
            .$ref("#/components/schemas/Pageable")
            .description("Thông tin phân trang"));
        
        // Metadata
        pageSchema.addProperty("totalElements", new IntegerSchema()
            .description("Tổng số items trong database")
            .example(15));
        
        pageSchema.addProperty("totalPages", new IntegerSchema()
            .description("Tổng số trang")
            .example(3));
        
        pageSchema.addProperty("size", new IntegerSchema()
            .description("Số items mỗi trang")
            .example(6));
        
        pageSchema.addProperty("number", new IntegerSchema()
            .description("Số trang hiện tại (bắt đầu từ 0)")
            .example(0));
        
        pageSchema.addProperty("numberOfElements", new IntegerSchema()
            .description("Số items trong trang hiện tại")
            .example(6));
        
        pageSchema.addProperty("first", new BooleanSchema()
            .description("Có phải trang đầu tiên không")
            .example(true));
        
        pageSchema.addProperty("last", new BooleanSchema()
            .description("Có phải trang cuối cùng không")
            .example(false));
        
        pageSchema.addProperty("empty", new BooleanSchema()
            .description("Trang có rỗng không")
            .example(false));
        
        pageSchema.addProperty("sort", new Schema<>()
            .$ref("#/components/schemas/Sort")
            .description("Thông tin sắp xếp"));
        
        return pageSchema;
    }

    /**
     * Schema cho Pageable object
     */
    private Schema<?> createPageableSchema() {
        Schema<?> pageableSchema = new ObjectSchema();
        
        pageableSchema.addProperty("pageNumber", new IntegerSchema()
            .description("Số trang (bắt đầu từ 0)")
            .example(0));
        
        pageableSchema.addProperty("pageSize", new IntegerSchema()
            .description("Số items mỗi trang")
            .example(6));
        
        pageableSchema.addProperty("offset", new IntegerSchema()
            .description("Vị trí bắt đầu trong tổng danh sách")
            .example(0));
        
        pageableSchema.addProperty("paged", new BooleanSchema()
            .description("Có phân trang hay không")
            .example(true));
        
        pageableSchema.addProperty("unpaged", new BooleanSchema()
            .description("Không phân trang")
            .example(false));
        
        pageableSchema.addProperty("sort", new Schema<>()
            .$ref("#/components/schemas/Sort"));
        
        return pageableSchema;
    }

    /**
     * Schema cho Sort object
     */
    private Schema<?> createSortSchema() {
        Schema<?> sortSchema = new ObjectSchema();
        
        sortSchema.addProperty("sorted", new BooleanSchema()
            .description("Có được sắp xếp không")
            .example(true));
        
        sortSchema.addProperty("unsorted", new BooleanSchema()
            .description("Không được sắp xếp")
            .example(false));
        
        sortSchema.addProperty("empty", new BooleanSchema()
            .description("Không có sắp xếp")
            .example(false));
        
        return sortSchema;
    }
}