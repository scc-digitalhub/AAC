import { AuthProvider, fetchUtils, Options } from 'react-admin';

const fetchJson = (url: string, options: Options = {}) => {
    if (!options.headers) {
        options.headers = new Headers({ Accept: 'application/json' });
    }

    options.credentials = 'include';

    return fetchUtils.fetchJson(url, options);
};

export default (baseUrl: string, httpClient = fetchJson): AuthProvider => {
    const apiUrl = baseUrl + '/console/user';

    return {
        login: async (params: any) => {},
        logout: async (params: any) => {
            window.location.href = `${baseUrl}/logout`;
            return;
        },
        checkError: async (error: any) => {
            const status = error.status;
            if (status === 401 || status === 403) {
                return Promise.reject();
            }
            // other error code (404, 500, etc): no need to log out
            return Promise.resolve();
        },
        checkAuth: async (params: any) => {
            const url = `${apiUrl}/status`;
            const { status } = await httpClient(url);
            if (status !== 200) {
                throw new Error('unauthorized');
            }
        },
        getIdentity: async () => {
            //load from profile
            let url = `${apiUrl}/me`;
            const { status, json } = await httpClient(url);

            if (status !== 200 || !json || !json.subjectId) {
                throw new Error('profile error');
            }

            //explode realm model
            url = `${apiUrl}/realm`;
            const { status: rstatus, json: rjson } = await httpClient(url);

            if (rstatus !== 200 || !rjson || !rjson.slug) {
                throw new Error('profile error');
            }

            return {
                id: json.subjectId,
                fullName: json.username,
                ...json,
                realm: rjson,
            };
        },
        getPermissions: async () => {
            //load from authorities
            const url = `${apiUrl}/authorities`;
            const { status, json } = await httpClient(url);
            if (status !== 200 || !json) {
                throw new Error('permissions error');
            }

            return json;
        },
    };
};
