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

export const GroupCreate = () => {
    const transform = (data: any) => {
        return {
            ...data,
            group: data.key
        }
    };
    return (
        <CreateBase
            transform={transform}
            redirect="list"
        >
            <GroupCreateForm />
        </CreateBase>
    );
};
export const GroupCreateForm = () => {
    return (
        <>
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
                                                    source="key"
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
            </>
    );
};
