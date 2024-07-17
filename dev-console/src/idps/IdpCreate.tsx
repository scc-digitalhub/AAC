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
import { useRootSelector } from '@dslab/ra-root-selector';

export const IdpCreate = () => {
    const { root: realmId } = useRootSelector();



    const transform = (data: any) => {
        return {
            ...data,
            type: 'identity',
            realm: realmId,
            configuration: { applicationType: data.type },
        };
        // let body = {};
        // body['name'] = data.name;
        // body['type'] = 'identity';
        //  body['realm'] =realmId;
        // body['authority'] = data.authority;
        // body['configuration'] = { applicationType: data.type };
        // return body;
    };

    return (
        <CreateBase
            transform={transform}
            redirect="list"
        >
            <IdpCreateForm />
        </CreateBase>
    );
};

export const IdpCreateForm = () => {
    return (
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
    );

}