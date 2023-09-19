import {
    BooleanInput,
    CreateBase,
    Form,
    TextInput,
    Toolbar,
    useNotify,
    useRedirect,
} from 'react-admin';
import { Card, CardContent, Box, Divider } from '@mui/material';
import { useParams } from 'react-router-dom';

export const RealmCreate = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const notify = useNotify();
    const redirect = useRedirect();

    const transform = (data: any) => {
        {
            console.log(data);
            let body = createRealm(data);
            console.log(body);
            return body;
        }
    };

    const onSuccess = (data: any) => {
        notify(`Realm created successfully`);
        redirect('list', 'myrealms');
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
                                                    source="slug"
                                                    fullWidth
                                                />
                                            </Box>
                                            <Box display="flex" width={430}>
                                                <TextInput
                                                    source="name"
                                                    fullWidth
                                                />
                                            </Box>
                                            <Box>
                                                <BooleanInput
                                                    source="public"
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

function createRealm(data: any): any {
    let body: any = {};
    body['slug'] = data.slug;
    body['name'] = data.name;
    body['public'] = data.hidden;
    return body;
}
