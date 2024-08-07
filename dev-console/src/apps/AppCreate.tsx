import { CreateBase, Form, SelectInput, TextInput, Toolbar } from 'react-admin';
import { Card, CardContent, Box, Divider } from '@mui/material';
import { Page } from '../components/page';

export const AppCreateForm = () => {
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
    );
};
