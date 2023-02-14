import { stringify } from 'querystring';
import { fetchUtils, DataProvider } from 'ra-core';
import jsonServerProvider from 'ra-data-json-server';
import { Options } from 'react-admin';

const fetchJson = (url: string, options: Options = {}) => {
    if (!options.headers) {
        options.headers = new Headers({ Accept: 'application/json' });
    }

    options.credentials = 'include';

    return fetchUtils.fetchJson(url, options);
};

export default (baseUrl: string, httpClient = fetchJson): DataProvider => {
    const apiUrl = baseUrl + '/console/admin';
    const provider = jsonServerProvider(apiUrl, httpClient);

    return {
        apiUrl: async () => apiUrl,
        invoke: ({
            path,
            params,
            body,
            options,
        }: {
            path: string;
            params?: any;
            body?: string;
            options?: Options;
        }) => {
            let url = `${apiUrl}/${path}`;
            if (params) {
                url = `${apiUrl}/${path}?${stringify(params)}`;
            }
            const opts = options ? options : {};
            if (body) {
                opts.body = body;
            }
            return httpClient(url, opts).then(({ headers, json }) => {
                return json;
            });
        },
        appProps: () => {
            const url = `${apiUrl}/props`;
            return httpClient(url).then(({ json }) => {
                return json;
            });
        },
        getList: (resource, params) => {
            const { page, perPage } = params.pagination;
            const { field, order } = params.sort;
            const query = {
                ...fetchUtils.flattenObject(params.filter),
                sort: field + ',' + order,
                page: page - 1,
                size: perPage,
            };
            const url = `${apiUrl}/${resource}?${stringify(query)}`;

            return httpClient(url).then(({ headers, json }) => {
                if (!json.content) {
                    throw new Error('the response must match page<> model');
                }
                return {
                    data: json.content,
                    total: parseInt(json.totalElements, 10),
                };
            });
        },
        getOne: (resource, params) => provider.getOne(resource, params),
        getMany: (resource, params) => provider.getMany(resource, params),
        getManyReference: (resource, params) => {
            const { page, perPage } = params.pagination;
            const { field, order } = params.sort;
            const query = {
                ...fetchUtils.flattenObject(params.filter),
                [params.target]: params.id,
                sort: field + ',' + order,
                page: page - 1,
                size: perPage,
            };
            const url = `${apiUrl}/${resource}?${stringify(query)}`;

            return httpClient(url).then(({ headers, json }) => {
                if (!json.content) {
                    throw new Error('the response must match page<> model');
                }
                return {
                    data: json.content,
                    total: parseInt(json.totalElements, 10),
                };
            });
        },
        update: (resource, params) => provider.update(resource, params),
        updateMany: (resource, params) => provider.updateMany(resource, params),
        create: (resource, params) => provider.create(resource, params),
        delete: (resource, params) => provider.delete(resource, params),
        deleteMany: (resource, params) => provider.deleteMany(resource, params),
    };
};
