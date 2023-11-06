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
    const apiUrl = baseUrl + '/console/dev';
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
        myAuthorities: () => {
            const url = `${apiUrl}/authorities`;
            return httpClient(url).then(({ json }) => {
                return json;
            });
        },
        getList: (resource, params) => {
            let url = `${apiUrl}/${resource}`;
            const { page, perPage } = params.pagination;
            const { field, order } = params.sort;
            const query = {
                ...fetchUtils.flattenObject(params.filter),
                sort: field + ',' + order,
                page: page - 1,
                size: perPage,
            };
            if (resource !== 'myrealms') {
                const realmId = params.meta.realmId;
                url = url + '/' + realmId;
            }
            url = url + `?${stringify(query)}`;
            return httpClient(url).then(({ headers, json }) => {
                if (json && Array.isArray(json)) {
                    return { data: json, total: json.length };
                }
                if (!json.content) {
                    throw new Error('the response must match page<> model');
                }
                return {
                    data: json.content,
                    total: parseInt(json.totalElements, 10),
                };
            });
        },
        getOne: (resource, params) => {
            let url = `${apiUrl}/${resource}`;
            if (resource !== 'myrealms') {
                const realmId = params.meta.realmId;
                url = url + '/' + realmId;
            }
            url = url + `/${params.id}`;
            return httpClient(url).then(({ status, json }) => {
                if (status !== 200) {
                    throw new Error('Invalid response status ' + status);
                }
                return {
                    data: json,
                };
            });
        },
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
        update: (resource, params) => {
            let url = `${apiUrl}/${resource}`;
            if (resource !== 'myrealms') {
                const realmId = params.meta.realmId;
                url = url + '/' + realmId;
            }
            url = url + `/${params.id}`;
            return httpClient(url, {
                method: 'PUT',
                body:
                    typeof params.data === 'string'
                        ? params.data
                        : JSON.stringify(params.data),
            }).then(({ json }) => ({ data: json }));
        },
        updateMany: (resource, params) => provider.updateMany(resource, params),
        create: (resource, params) => {
            let method = `POST`;
            let url = `${apiUrl}/${resource}`;
            let headers = {
                'Access-Control-Allow-Origin': '*',
            };
            let body: any;
            if (resource !== 'myrealms') {
                const realmId = params.meta.realmId;
                url = url + '/' + realmId;
            }
            if (params.meta.import) {
                method = `PUT`;
                let formData = new FormData();
                formData.append('yaml', String(params.data));
                body = formData;
                url = url + '?reset=' + params.meta.resetId;
            } else {
                body = JSON.stringify(params.data);
            }
            return httpClient(url, {
                method: method,
                headers: new Headers(headers),
                body: body,
            }).then(({ json }) => ({
                data: { ...params.data, id: json.id } as any,
            }));
        },
        delete: (resource, params) => {
            let url = `${apiUrl}/${resource}`;

            if (resource !== 'myrealms') {
                const realmId = params.meta.rootId;
                url = url + '/' + realmId;
            }

            url = url + `/${params.id}`;

            return httpClient(url, {
                method: 'DELETE',
            }).then(({ json }) => ({ data: json }));
        },
        deleteMany: (resource, params) => provider.deleteMany(resource, params),
    };
};
