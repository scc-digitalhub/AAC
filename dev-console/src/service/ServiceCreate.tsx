import {
    CreateBase,
    Form,
    TextInput,
    Toolbar,
    useNotify,
    useRedirect,
} from 'react-admin';
import { Card, CardContent, Box, Divider } from '@mui/material';
import { useParams } from 'react-router-dom';

export const ServiceCreate = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const notify = useNotify();
    const redirect = useRedirect();

    const transform = (data: any) => {
        let body = createService(data, params.realmId);
        return body;
    };

    const onSuccess = (data: any) => {
        notify(`Service created successfully`);
        redirect(`/services/r/${params.realmId}`);
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
                                                <TextInput
                                                    source="description"
                                                    fullWidth
                                                />
                                            </Box>
                                            <Box display="flex" width={430}>
                                                <TextInput
                                                    source="namespace"
                                                    fullWidth
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

function createService(data: any, realmId: any): any {
    let body: any = {};
    body['name'] = data.name;
    body['description'] = data.description;
    body['namespace'] = data.namespace;
    return body;
}
