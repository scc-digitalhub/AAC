import { List, Datagrid, TextField } from 'react-admin';
import { useParams } from 'react-router-dom';

export const DebugList = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };

    return (
        <List queryOptions={options}>
            <Datagrid>
                <TextField source="id" />
            </Datagrid>
        </List>
    );
};
