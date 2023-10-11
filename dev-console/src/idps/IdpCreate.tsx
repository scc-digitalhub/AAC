import {
    CreateBase,
    Form,
    SelectInput,
    TextInput,
    Toolbar,
    useNotify,
    useRedirect,
} from 'react-admin';
import { Card, CardContent, Box, Divider } from '@mui/material';
import { useParams } from 'react-router-dom';

export const IdpCreate = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const notify = useNotify();
    const redirect = useRedirect();

    const transform = (data: any) => {
        let body = createIdp(data, params.realmId);
        return body;
    };

    const onSuccess = (data: any) => {
        notify(`Provider created successfully`);
        redirect(`/idps/r/${params.realmId}`);
    };
    return (
        <CreateBase
            transform={transform}
            mutationOptions={{ ...options, onSuccess }}
        >
            <Box mt={2} display="flex">
                <Box flex="1">
                    <Form>
                        <Card>
                            <CardContent>
                                <Box>
                                    <Box display="flex">
                                        <Box flex="1" mt={-1}>
                                            <Box display="flex" width={430}>
                                                <TextInput
                                                    source="name"
                                                    fullWidth
                                                />
                                            </Box>
                                            <Box display="flex" width={430}>
                                                <SelectInput
                                                    defaultValue={'apple'}
                                                    source="authority"
                                                    label="Authority"
                                                    choices={[
                                                        {
                                                            id: 'apple',
                                                            name: 'Apple',
                                                        },
                                                        {
                                                            id: 'internal',
                                                            name: 'Internal',
                                                        },
                                                        {
                                                            id: 'password',
                                                            name: 'Password',
                                                        },
                                                        {
                                                            id: 'github',
                                                            name: 'Github',
                                                        },
                                                        {
                                                            id: 'facebook',
                                                            name: 'Facebook',
                                                        },
                                                        {
                                                            id: 'saml',
                                                            name: 'Saml',
                                                        },
                                                        {
                                                            id: 'webauthn',
                                                            name: 'WebAuthn',
                                                        },
                                                        {
                                                            id: 'google',
                                                            name: 'Google',
                                                        },
                                                        {
                                                            id: 'oidc',
                                                            name: 'Oidc',
                                                        },
                                                    ]}
                                                />
                                            </Box>
                                            <Divider />
                                        </Box>
                                    </Box>
                                </Box>
                            </CardContent>
                            <Toolbar />
                        </Card>
                    </Form>
                </Box>
            </Box>
        </CreateBase>
    );
};

function createIdp(data: any, realmId: any): any {
    let body: any = {};
    body['name'] = data.name;
    body['type'] = 'identity';
    body['realm'] = realmId;
    body['authority'] = data.authority;
    body['configuration'] = { applicationType: data.type };
    return body;
}
