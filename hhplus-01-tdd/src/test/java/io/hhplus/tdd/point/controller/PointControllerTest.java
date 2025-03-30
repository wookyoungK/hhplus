package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
        userPointTable.insertOrUpdate(1L, 500L);
    }
    @Test
    @DisplayName("특정 유저의 포인트를 조회하는 API 기능 테스트")
    void getUserPoint() throws Exception {
        // when & then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/point/{id}", 1L)
        ).andExpect(
                status().isOk()
        ).andExpect(
                jsonPath("$.id").value(1)
        ).andExpect(
                jsonPath("$.point").value(500) // 설정한 포인트 정보 검증
        ).andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("특정 유저의 히스토리를 조회하는 API 기능 테스트")
    void getUserHistory() throws Exception {
        // when & then
        mockMvc.perform(
                MockMvcRequestBuilders.get("/point/{id}/histories", 1L)
        ).andExpect(
                status().isOk()
        ).andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("특정 유저의 포인트를 충전하는 API 기능 테스트")
    void chargeUserPoints() throws Exception {
        // when & then
        mockMvc.perform(
                MockMvcRequestBuilders.patch("/point/{id}/charge", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1000")  // 충전할 포인트 양
        ).andExpect(
                status().isOk()
        ).andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("특정 유저의 포인트를 사용하는 API 기능 테스트")
    void useUserPoints() throws Exception {
        // when & then
        mockMvc.perform(
                MockMvcRequestBuilders.patch("/point/{id}/use", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("300")  // 사용할 포인트 양
        ).andExpect(
                status().isOk()
        ).andDo(MockMvcResultHandlers.print());
    }
}