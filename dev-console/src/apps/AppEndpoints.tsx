import { useRootSelector } from '@dslab/ra-root-selector';
import { Stack } from '@mui/material';
import { useState, useEffect } from 'react';
import {
    useDataProvider,
    RecordContextProvider,
    Labeled,
    TextField,
} from 'react-admin';

export const AppEndpointsView = () => {
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const [config, setConfig] = useState<any>();

    useEffect(() => {
        dataProvider
            .invoke({ path: 'realms/' + realmId + '/well-known/oauth2' })
            .then(data => {
                if (data) {
                    setConfig(data);
                }
            });
    }, [dataProvider]);

    if (!config) return null;

    const fields = [
        'issuer',
        'authorization_endpoint',
        'token_endpoint',
        'jwks_uri',
        'userinfo_endpoint',
        'introspection_endpoint',
        'revocation_endpoint',
        'registration_endpoint',
        'end_session_endpoint',
    ];

    return (
        <RecordContextProvider value={config}>
            <Stack>
                {fields.map(field => {
                    return (
                        <Labeled key={'endpoints.' + field}>
                            <TextField source={field} />
                        </Labeled>
                    );
                })}
            </Stack>
        </RecordContextProvider>
    );
};
