import { CreateBase, Form, SaveButton, TextInput, Toolbar } from 'react-admin';
import { Card, CardContent, Box, Divider, Typography } from '@mui/material';

export const UserCreate = () => {
    const transform = (data: any) => {
        return {
            ...data,
        };
    };

    return <CreateBase transform={transform}><UserCreateForm/></CreateBase>;
};
export const UserCreateForm = () => {
    return (
        <>
            <Typography>Invite user</Typography>
            <Box mt={2} display="flex">
                <Box flex="1">
                    <Form>
                        <Card>
                            <CardContent>
                                <Box>
                                    <Box display="flex">
                                        <Box flex="1">
                                            <Box display="flex">
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
                                <SaveButton />
                            </Toolbar>
                        </Card>
                    </Form>
                </Box>
            </Box>
        </>
    );
};
