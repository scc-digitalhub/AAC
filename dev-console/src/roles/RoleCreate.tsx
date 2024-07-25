import {
    CreateBase,
    Form,
    TextInput,
    Toolbar,
} from 'react-admin';
import { Card, CardContent, Box, Divider } from '@mui/material';

export const RoleCreate = () => {

    const transform = (data: any) => {
        return {
            ...data
        };
    };
    return (
        <CreateBase
            transform={transform}
        >
            <RoleCreateForm />
        </CreateBase>
    );
};

export const RoleCreateForm = () => {
    return (<Box mt={2} display="flex">
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
                                        source="role"
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
}