import {
    EditBase,
    Form,
    TextInput,
    useEditContext,
    BooleanInput,
    useNotify,
    useRedirect,
    Toolbar,
} from 'react-admin';
import { Card, CardContent, Box, Divider } from '@mui/material';
import { useParams } from 'react-router';
import React from 'react';

export const RealmEdit = () => {
    const notify = useNotify();
    const redirect = useRedirect();
    const params = useParams();
    const options = { meta: { realmId: params.id } };

    const onSuccess = (data: any) => {
        notify(`Realm created successfully`);
        redirect('list', 'myrealms');
    };

    const transform = (data: any) => {
        {
            console.log(data);
            let body = createRealm(data);
            console.log(body);
            return body;
        }
    };

    return (
        <EditBase
            mutationMode="pessimistic"
            redirect="list"
            transform={transform}
            mutationOptions={{ ...options, onSuccess }}
            queryOptions={options}
        >
            <RealmEditContent />
        </EditBase>
    );
};

const RealmEditContent = () => {
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;

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
    );
};

function createRealm(data: any): any {
    let body: any = {};
    body['slug'] = data.slug;
    body['name'] = data.name;
    body['public'] = data.public;
    return body;
}
