import {
    CreateBase,
    Form,
    SaveButton,
    TextInput,
    Toolbar,
    useNotify,
    useRedirect,
} from 'react-admin';
import { Card, CardContent, Box, Divider, Typography } from '@mui/material';
import { useParams } from 'react-router-dom';

export const UserCreate = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const notify = useNotify();
    const redirect = useRedirect();

    const transform = (data: any) => {
        let body = createService(data, params.realmId);
        return body;
    };

    const onSuccess = (data: any) => {
        notify(`User invited successfully`);
        redirect(`/users/r/${params.realmId}`);
    };
    return (
        <CreateBase
            transform={transform}
            mutationOptions={{ ...options, onSuccess }}
        >
            <Typography sx={{ 'font-weight': 400, mt: 2 }}>
                Invite user
            </Typography>
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
                                                    source="email"
                                                    fullWidth
                                                />
                                            </Box>
                                            <Divider />
                                        </Box>
                                    </Box>
                                </Box>
                            </CardContent>
                            <Toolbar>
                                <SaveButton icon={<></>} label="Invite" />
                            </Toolbar>
                        </Card>
                    </Form>
                </Box>
            </Box>
        </CreateBase>
    );
};

function createService(data: any, realmId: any): any {
    let body: any = {};
    body['email'] = data.email;
    return body;
}
