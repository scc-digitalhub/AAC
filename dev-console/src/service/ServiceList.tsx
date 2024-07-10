import {
    List,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    Empty,
} from 'react-admin';
import { useParams } from 'react-router-dom';

export const ServiceList = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };

    return (
        <List
            queryOptions={options}
            empty={<Empty />}
            actions={<ServiceListActions />}
        >
            <Datagrid>
                <TextField source="id" />
            </Datagrid>
        </List>
    );
};

const ServiceListActions = () => {
    const params = useParams();
    const to = `/services/r/${params.realmId}/create`;
    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="New Service"
                sx={{ marginLeft: 2 }}
                to={to}
            />
        </TopToolbar>
    );
};
