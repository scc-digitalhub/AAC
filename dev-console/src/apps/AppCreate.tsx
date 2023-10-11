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

export const AppCreate = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const notify = useNotify();
    const redirect = useRedirect();

    const transform = (data: any) => {
        let body = createApp(data, params.realmId);
        return body;
    };

    const onSuccess = (data: any) => {
        notify(`App created successfully`);
        redirect(`/apps/r/${params.realmId}`);
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
                                                    defaultValue={'web'}
                                                    source="type"
                                                    label="Type"
                                                    choices={[
                                                        {
                                                            id: 'web',
                                                            name: 'Web',
                                                        },
                                                        {
                                                            id: 'native',
                                                            name: 'Native',
                                                        },
                                                        {
                                                            id: 'machine',
                                                            name: 'Machine',
                                                        },
                                                        {
                                                            id: 'spa',
                                                            name: 'SPA',
                                                        },
                                                        {
                                                            id: 'introspection',
                                                            name: 'Introspection',
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

function createApp(data: any, realmId: any): any {
    let body: any = {};
    body['name'] = data.name;
    body['type'] = 'oauth2';
    body['realm'] = realmId;
    body['configuration'] = { applicationType: data.type };
    return body;
}
