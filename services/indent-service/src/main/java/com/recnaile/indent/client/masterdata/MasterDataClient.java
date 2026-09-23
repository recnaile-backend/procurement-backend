package com.recnaile.indent.client.masterdata;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

public class MasterDataClient {

    private final RestClient restClient;

    public MasterDataClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public DepartmentResponse getDepartment(String departmentId) {
        try {
            DepartmentResponse response = restClient.get()
                    .uri("/api/v1/master-data/departments/{departmentId}", departmentId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response1) -> {
                        if (response1.getStatusCode().value() == 404) {
                            throw new MasterDataException(
                                    MasterDataException.Reason.NOT_FOUND,
                                    departmentId,
                                    "Department was not found: " + departmentId);
                        }
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                departmentId,
                                "Master Data rejected department lookup");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response1) -> {
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                departmentId,
                                "Master Data service is unavailable");
                    })
                    .body(DepartmentResponse.class);

            if (response == null) {
                throw new MasterDataException(
                        MasterDataException.Reason.INVALID_RESPONSE,
                        departmentId,
                        "Master Data returned an empty department response");
            }

            return response;
        } catch (MasterDataException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new MasterDataException(
                    MasterDataException.Reason.UNAVAILABLE,
                    departmentId,
                    "Unable to reach Master Data service for department",
                    ex);
        }
    }


    public List<DepartmentResponse> getAllDepartments() {
        try {
            List<DepartmentResponse> response = restClient.get()
                    .uri("/api/v1/master-data/departments")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response1) -> {
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                "departments",
                                "Master Data rejected department list lookup");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response1) -> {
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                "departments",
                                "Master Data service is unavailable");
                    })
                    .body(new org.springframework.core.ParameterizedTypeReference<List<DepartmentResponse>>() {});

            if (response == null) {
                throw new MasterDataException(
                        MasterDataException.Reason.INVALID_RESPONSE,
                        "departments",
                        "Master Data returned an empty department list response");
            }

            return response;
        } catch (MasterDataException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new MasterDataException(
                    MasterDataException.Reason.UNAVAILABLE,
                    "departments",
                    "Unable to reach Master Data service for departments",
                    ex);
        }
    }

    public ProjectResponse getProject(String projectId) {
        try {
            ProjectResponse response = restClient.get()
                    .uri("/api/v1/master-data/projects/{projectId}", projectId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response1) -> {
                        if (response1.getStatusCode().value() == 404) {
                            throw new MasterDataException(
                                    MasterDataException.Reason.NOT_FOUND,
                                    projectId,
                                    "Project was not found: " + projectId);
                        }
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                projectId,
                                "Master Data rejected project lookup");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response1) -> {
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                projectId,
                                "Master Data service is unavailable");
                    })
                    .body(ProjectResponse.class);

            if (response == null) {
                throw new MasterDataException(
                        MasterDataException.Reason.INVALID_RESPONSE,
                        projectId,
                        "Master Data returned an empty project response");
            }

            return response;
        } catch (MasterDataException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new MasterDataException(
                    MasterDataException.Reason.UNAVAILABLE,
                    projectId,
                    "Unable to reach Master Data service for project",
                    ex);
        }
    }

    public List<ItemMasterResponse> getAllItems() {
        try {
            List<ItemMasterResponse> response = restClient.get()
                    .uri("/api/v1/master-data/items")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response1) -> {
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                "items",
                                "Master Data rejected item list lookup");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response1) -> {
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                "items",
                                "Master Data service is unavailable");
                    })
                    .body(new org.springframework.core.ParameterizedTypeReference<List<ItemMasterResponse>>() {});

            if (response == null) {
                throw new MasterDataException(
                        MasterDataException.Reason.INVALID_RESPONSE,
                        "items",
                        "Master Data returned an empty item list response");
            }

            return response;
        } catch (MasterDataException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new MasterDataException(
                    MasterDataException.Reason.UNAVAILABLE,
                    "items",
                    "Unable to reach Master Data service for items",
                    ex);
        }
    }

    public ItemMasterResponse getItem(String itemId) {
        try {
            ItemMasterResponse response = restClient.get()
                    .uri("/api/v1/master-data/items/{itemId}", itemId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response1) -> {
                        if (response1.getStatusCode().value() == 404) {
                            throw new MasterDataException(
                                    MasterDataException.Reason.NOT_FOUND,
                                    itemId,
                                    "Item was not found: " + itemId);
                        }
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                itemId,
                                "Master Data rejected item lookup");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response1) -> {
                        throw new MasterDataException(
                                MasterDataException.Reason.UNAVAILABLE,
                                itemId,
                                "Master Data service is unavailable");
                    })
                    .body(ItemMasterResponse.class);

            if (response == null) {
                throw new MasterDataException(
                        MasterDataException.Reason.INVALID_RESPONSE,
                        itemId,
                        "Master Data returned an empty item response");
            }

            return response;
        } catch (MasterDataException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new MasterDataException(
                    MasterDataException.Reason.UNAVAILABLE,
                    itemId,
                    "Unable to reach Master Data service for item",
                    ex);
        }
    }

    public static SimpleClientHttpRequestFactory requestFactory(
            Duration connectTimeout,
            Duration readTimeout) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
