import { useRecordContext } from 'react-admin';
import { PageTitle } from '../components/pageTitle';
import { AppIcon } from './AppIcon';

export const AppTitle = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <PageTitle
            text={record.name}
            secondaryText={record?.id}
            copy={true}
            icon={<AppIcon fontSize="large" sx={{ fontSize: '96px' }} />}
        />
    );
};
