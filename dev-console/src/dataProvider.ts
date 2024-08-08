import { stringify } from 'query-string';
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
            const { page, perPage } = params.pagination;
            const { field, order } = params.sort;
            const query = {
                ...fetchUtils.flattenObject(params.filter),
                sort: field + ',' + order,
                page: page - 1,
                size: perPage,
            };
            let suffix = '';
            if (resource !== 'myrealms' && params.meta?.root) {
                suffix = '/' + params.meta.root;
            }
            const url =
                `${apiUrl}/${resource}${suffix}` + `?${stringify(query)}`;
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
            let suffix = '';
            if (resource !== 'myrealms' && params.meta?.root) {
                suffix = '/' + params.meta.root;
            }

            const flatten = params.meta?.flatten || [];

            const url = `${apiUrl}/${resource}${suffix}` + `/${params.id}`;
            return httpClient(url).then(({ status, json }) => {
                if (status !== 200) {
                    throw new Error('Invalid response status ' + status);
                }

                //flatten nested fields if required
                if (json) {
                    for (const k of flatten) {
                        if (k in json) {
                            if (Array.isArray(json[k])) {
                                json[k] = json[k].map(e => {
                                    return typeof e === 'object' && 'id' in e
                                        ? e['id']
                                        : e;
                                });
                            } else if (
                                typeof json[k] === 'object' &&
                                'id' in json[k]
                            ) {
                                json[k] = json[k]['id'];
                            }
                        }
                    }
                }

                return {
                    data: json,
                };
            });
        },
        getMany: (resource, params) => {
            //no pagination!
            let prefix = '';
            if (resource !== 'myrealms' && params.meta?.root) {
                prefix = '/' + params.meta.root;
            }
            const url = `${apiUrl}/${resource}${prefix}`;

            return Promise.all(
                params.ids.map(id => {
                    //remap id from nested objects if necessary
                    if (typeof id === 'object' && 'id' in id) {
                        return httpClient(`${url}/${id['id']}`);
                    }

                    return httpClient(`${url}/${id}`);
                })
            ).then(responses => ({
                data: responses.map(({ json }) => json),
            }));
        },

        //TODO anche qui da fixare con param
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
            let prefix = '';
            if (resource !== 'myrealms' && params.meta?.root) {
                prefix = '/' + params.meta.root;
            }
            const url =
                `${apiUrl}/${resource}${prefix}` + `?${stringify(query)}`;
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
        update: (resource, params) => {
            let suffix = '';
            if (resource !== 'myrealms' && params.meta?.root) {
                suffix = '/' + params.meta.root;
            }

            const flatten = params.meta?.flatten || [];

            const url = `${apiUrl}/${resource}${suffix}` + `/${params.id}`;
            return httpClient(url, {
                method: 'PUT',
                body:
                    typeof params.data === 'string'
                        ? params.data
                        : JSON.stringify(params.data),
            }).then(({ status, json }) => {
                if (status !== 200) {
                    throw new Error('Invalid response status ' + status);
                }

                //flatten nested fields if required
                if (json) {
                    for (const k of flatten) {
                        if (k in json) {
                            if (Array.isArray(json[k])) {
                                json[k] = json[k].map(e => {
                                    return typeof e === 'object' && 'id' in e
                                        ? e['id']
                                        : e;
                                });
                            } else if (
                                typeof json[k] === 'object' &&
                                'id' in json[k]
                            ) {
                                json[k] = json[k]['id'];
                            }
                        }
                    }
                }

                return {
                    data: json,
                };
            });
        },
        updateMany: (resource, params) => {
            let prefix = '';
            if (resource !== 'myrealms' && params.meta?.root) {
                prefix = '/' + params.meta.root;
            }
            const url = `${apiUrl}${prefix}/${resource}`;

            //make a distinct call for every entry
            return Promise.all(
                params.ids.map(id =>
                    httpClient(`${url}/${id}`, {
                        method: 'PUT',
                        body: JSON.stringify(params.data),
                    })
                )
            ).then(responses => ({
                data: responses.map(({ json }) => json.id),
            }));
        },
        create: (resource, params) => {
            let method = `POST`;
            let headers = {
                'Access-Control-Allow-Origin': '*',
            };
            let body: any;

            let suffix = '';
            if (resource !== 'myrealms' && params.meta?.root) {
                suffix = '/' + params.meta.root;
            }
            let url = `${apiUrl}/${resource}${suffix}`;

            if (params.meta.import) {
                method = `PUT`;
                let formData = new FormData();
                formData.append('yaml', String(params.data));
                body = formData;
                url = url + '?reset=' + params.meta.resetId;
            } else {
                body = JSON.stringify(params?.data);
            }
            return httpClient(url, {
                method: method,
                headers: new Headers(headers),
                body: body,
            }).then(({ json }) => ({
                data: { ...params.data, id: json?.id } as any,
            }));
        },
        delete: (resource, params) => {
            let suffix = '';
            if (resource !== 'myrealms' && params.meta?.root) {
                suffix = '/' + params.meta.root;
            }
            const url = `${apiUrl}/${resource}${suffix}` + `/${params.id}`;

            return httpClient(url, {
                method: 'DELETE',
            }).then(({ json }) => ({ data: json }));
        },
        deleteMany: (resource, params) => {
            let prefix = '';
            if (resource !== 'myrealms' && params.meta?.root) {
                prefix = '/' + params.meta.root;
            }
            const url = `${apiUrl}${prefix}/${resource}`;

            //make a distinct call for every entry
            return Promise.all(
                params.ids.map(id =>
                    httpClient(`${url}/${id}`, {
                        method: 'DELETE',
                    })
                )
            ).then(responses => ({ data: responses.map(({ json }) => json) }));
        },
    };
};
